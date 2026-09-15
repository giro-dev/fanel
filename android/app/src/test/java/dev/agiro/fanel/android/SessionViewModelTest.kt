package dev.agiro.fanel.android

import dev.agiro.fanel.android.data.remote.HouseholdApi
import dev.agiro.fanel.android.data.remote.HouseholdDto
import dev.agiro.fanel.android.data.remote.MemberDto
import dev.agiro.fanel.android.data.remote.VerifyPinRequest
import dev.agiro.fanel.android.data.remote.VerifyPinResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SessionViewModelTest {
    private val household = HouseholdDto("household-1", "Casa", "ca", "Europe/Madrid", null)

    private fun viewModel(
        members: List<MemberDto>,
        pinChecker: (String) -> Boolean = { true }
    ): Pair<SessionViewModel, SessionStore> {
        val context = RuntimeEnvironment.getApplication()
        val sessionStore = SessionStore(context).apply {
            householdId = household.id
            householdName = household.name
        }
        val container = FakeAppContainer(
            context,
            sessionStore = sessionStore,
            householdApi = object : HouseholdApi {
                override suspend fun list() = listOf(household)
                override suspend fun members(householdId: String) = members
                override suspend fun verifyPin(
                    householdId: String,
                    memberId: String,
                    request: VerifyPinRequest
                ) = VerifyPinResponse(pinChecker(request.pin))
            }
        )
        return SessionViewModel(context, container) to sessionStore
    }

    @Test
    fun pickMemberWithoutPinStoresMember() = runBlocking {
        val member = member("m1", "Albert", hasPin = false)
        val (vm, store) = viewModel(listOf(member))

        assertTrue(vm.pickMember(member, null))

        assertEquals("m1", store.memberId)
        assertEquals("Albert", store.memberName)
        assertEquals("m1", vm.uiState.value.memberId)
    }

    @Test
    fun pickMemberWithValidPinStoresMember() = runBlocking {
        val member = member("m1", "Albert", hasPin = true)
        val (vm, store) = viewModel(listOf(member), pinChecker = { it == "1234" })

        assertTrue(vm.pickMember(member, "1234"))
        assertEquals("m1", store.memberId)
    }

    @Test
    fun pickMemberWithWrongPinFails() = runBlocking {
        val member = member("m1", "Albert", hasPin = true)
        val (vm, store) = viewModel(listOf(member), pinChecker = { it == "1234" })

        assertFalse(vm.pickMember(member, "9999"))
        assertEquals("", store.memberId)
        assertEquals("", vm.uiState.value.memberId)
    }

    @Test
    fun clearMemberResetsSelection() = runBlocking {
        val member = member("m1", "Albert", hasPin = false)
        val (vm, store) = viewModel(listOf(member))
        vm.pickMember(member, null)

        vm.clearMember()

        assertEquals("", store.memberId)
        assertEquals("", vm.uiState.value.memberId)
    }

    @Test
    fun selectHouseholdResetsMember() = runBlocking {
        val member = member("m1", "Albert", hasPin = false)
        val (vm, store) = viewModel(listOf(member))
        vm.pickMember(member, null)

        vm.selectHousehold(household)

        assertEquals("", store.memberId)
        assertEquals("", vm.uiState.value.memberId)
    }

    private fun member(id: String, name: String, hasPin: Boolean) = MemberDto(
        id, household.id, name, "ADULT", null, null, null, hasPin, false
    )
}
