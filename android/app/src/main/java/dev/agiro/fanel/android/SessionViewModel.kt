package dev.agiro.fanel.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.agiro.fanel.android.data.remote.ApiFactory
import dev.agiro.fanel.android.data.remote.HouseholdDto
import dev.agiro.fanel.android.data.remote.MemberDto
import dev.agiro.fanel.android.data.remote.VerifyPinRequest
import dev.agiro.fanel.android.wireguard.LOGICAL_BASE_URL
import dev.agiro.fanel.android.wireguard.ConnectionStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class SessionUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val apiToken: String = "",
    val connecting: Boolean = false,
    val errorRes: Int? = null,
    val households: List<HouseholdDto>? = null,
    val configured: Boolean = false,
    val paired: Boolean = false,
    val sessionKey: String = "",
    val householdName: String = "",
    val memberId: String = "",
    val memberName: String = ""
)

class SessionViewModel(
    application: Application,
    private val container: AppContainerContract = (application as FanelApplication).appContainer
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()
    val connectionStateHolder: ConnectionStateHolder
        get() = container.connectionStateHolder

    fun updateServerUrl(value: String) = _uiState.update {
        if (it.paired) it else it.copy(serverUrl = value, errorRes = null)
    }
    fun updateUsername(value: String) = _uiState.update { it.copy(username = value, errorRes = null) }
    fun updatePassword(value: String) = _uiState.update { it.copy(password = value, errorRes = null) }
    fun updateApiToken(value: String) = _uiState.update { it.copy(apiToken = value, errorRes = null) }

    fun connect() {
        val state = _uiState.value
        if (state.connecting) return
        if (state.serverUrl.isBlank() ||
            (state.apiToken.isBlank() && (state.username.isBlank() || state.password.isBlank()))
        ) {
            _uiState.update { it.copy(errorRes = R.string.setup_missing_credentials) }
            return
        }
        _uiState.update { it.copy(connecting = true, errorRes = null, households = null) }
        viewModelScope.launch {
            try {
                val paired = container.wgConfigRepository.current != null
                val url = if (paired) LOGICAL_BASE_URL else normalizeUrl(state.serverUrl)
                val households = if (paired) {
                    setCredentials(container.authStore, state)
                    container.sessionStore.serverUrl = LOGICAL_BASE_URL
                    container.refreshConnection()
                    container.householdApi.list()
                } else {
                    val probe = AuthStore()
                    setCredentials(probe, state)
                    val result = ApiFactory.householdApi(url, probe).list()
                    setCredentials(container.authStore, state)
                    result
                }
                if (!paired) container.sessionStore.serverUrl = url
                container.refreshConnection()

                _uiState.update {
                    it.copy(
                        connecting = false,
                        serverUrl = if (paired) url else it.serverUrl,
                        paired = paired,
                        households = households,
                        errorRes = null
                    )
                }
            } catch (_: HttpException) {
                if (container.wgConfigRepository.current != null) {
                    container.authStore.clear()
                    container.refreshConnection()
                }
                _uiState.update { it.copy(connecting = false, errorRes = R.string.setup_auth_error) }
            } catch (_: Exception) {
                if (container.wgConfigRepository.current != null) {
                    container.authStore.clear()
                    container.refreshConnection()
                }
                _uiState.update { it.copy(connecting = false, errorRes = R.string.setup_connection_error) }
            }
        }
    }

    fun selectHousehold(household: HouseholdDto) {
        container.sessionStore.householdId = household.id
        container.sessionStore.householdName = household.name
        container.sessionStore.memberId = ""
        container.sessionStore.memberName = ""
        _uiState.update {
            it.copy(
                configured = true,
                sessionKey = "${container.sessionStore.serverUrl}|${household.id}",
                householdName = household.name,
                memberId = "",
                memberName = ""
            )
        }
    }

    suspend fun listHouseholds(): List<HouseholdDto> = container.householdApi.list()

    suspend fun listMembers(): List<MemberDto> {
        val householdId = container.sessionStore.householdId
        if (householdId.isBlank()) return emptyList()
        return container.membersRepository.members(householdId)
    }

    suspend fun pickMember(member: MemberDto, pin: String?): Boolean {
        if (member.hasPin) {
            val result = container.householdApi.verifyPin(
                member.householdId, member.id, VerifyPinRequest(pin.orEmpty())
            )
            if (!result.valid) return false
        }
        container.sessionStore.memberId = member.id
        container.sessionStore.memberName = member.name
        _uiState.update { it.copy(memberId = member.id, memberName = member.name) }
        return true
    }

    fun clearMember() {
        container.sessionStore.memberId = ""
        container.sessionStore.memberName = ""
        _uiState.update { it.copy(memberId = "", memberName = "") }
    }

    fun logout() {
        container.authStore.clear()
        container.sessionStore.clear()
        container.refreshConnection()
        _uiState.value = initialState()
    }

    fun paired() {
        _uiState.update {
            it.copy(serverUrl = LOGICAL_BASE_URL, paired = true, errorRes = null)
        }
    }

    fun unpaired() {
        container.authStore.clear()
        val serverUrl = container.sessionStore.serverUrl
        container.sessionStore.clear()
        container.sessionStore.serverUrl = serverUrl
        container.refreshConnection()
        _uiState.value = initialState()
    }

    private fun initialState(): SessionUiState {
        val store = container.sessionStore
        val configured = store.householdId.isNotBlank() && container.authStore.isConfigured
        return SessionUiState(
            serverUrl = store.serverUrl,
            paired = container.wgConfigRepository.current != null,
            configured = configured,
            sessionKey = if (configured) "${store.serverUrl}|${store.householdId}" else "",
            householdName = store.householdName,
            memberId = store.memberId,
            memberName = store.memberName
        )
    }

    private fun normalizeUrl(raw: String): String {
        var url = raw.trim()
        if (!url.endsWith("/")) url += "/"
        return url
    }

    private fun setCredentials(store: AuthStore, state: SessionUiState) {
        if (state.apiToken.isNotBlank()) {
            store.setBearerToken(state.apiToken.trim())
        } else {
            store.setBasicCredentials(state.username.trim(), state.password)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SessionViewModel(application) as T
    }
}
