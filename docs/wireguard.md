# WireGuard transparent

Aquesta guia descriu la topologia A recomanada: WireGuard s'executa al mateix host que Fanel i l'app mòbil arriba al servidor per HTTPS a través del túnel. L'API genera una configuració de client d'un sol ús mitjançant un QR; les claus privades dels clients no passen mai pel servidor.

## Topologia A: Fanel i WireGuard al mateix host

```text
App Android ── HTTPS/8443 ── túnel WireGuard ── wg0 (10.8.0.1)
      │                                           │
      └── UDP/51820 ── Internet/router ───────────┘
                                                  │
                                    Fanel + Postgres al host
```

El servidor només publica UDP 51820 al router. El túnel dels clients només permet HTTPS al servidor; no és una VPN general ni dona accés a la LAN.

### Generar les claus del servidor i configurar wg0

Instal·la WireGuard (`wireguard-tools` al host) i crea la clau privada amb permisos restrictius:

```sh
sudo install -d -m 700 /etc/wireguard
umask 077
wg genkey | sudo tee /etc/wireguard/server.key >/dev/null
sudo chmod 600 /etc/wireguard/server.key
sudo sh -c 'wg pubkey < /etc/wireguard/server.key > /etc/wireguard/server.pub'
sudo cat /etc/wireguard/server.pub
```

La clau pública del servidor és necessària per a l'aplicació. Crea `/etc/wireguard/wg0.conf` amb propietari `root`, mode `600` i **sense cap secció `[Peer]`**: els peers els gestiona Fanel.

```ini
[Interface]
Address = 10.8.0.1/24
ListenPort = 51820
PrivateKey = <contingut de /etc/wireguard/server.key>
MTU = 1280
```

Activa la interfície a l'arrencada:

```sh
sudo systemctl enable --now wg-quick@wg0
sudo wg show wg0
```

Configura el router per reenviar UDP 51820 a la IP LAN fixa del servidor. Si la IP pública canvia, configura DDNS i posa el nom DNS a `FANEL_WG_ENDPOINT` (amb el port, per exemple `vpn.example.net:51820`). Amb CGNAT no es pot fer un reenviament entrant normal: cal una IP pública, un túnel sortint a un servidor amb IP pública o una altra solució de xarxa.

### Tallafoc nftables

Adapta les regles existents del host (especialment SSH i connexions establertes) abans d'aplicar-les. La política d'exemple només accepta l'handshake UDP i HTTPS al servidor des del túnel; no obre forwarding ni altres ports de Fanel als clients:

```nft
table inet filter {
  chain input {
    type filter hook input priority 0; policy drop;
    ct state established,related accept
    iifname "lo" accept
    # Mantén només els ports d'administració necessaris, limitats a la LAN.
    ip saddr 192.168.1.0/24 tcp dport 22 accept
    udp dport 51820 accept
    iifname "wg0" ip saddr 10.8.0.0/24 ip daddr 10.8.0.1 tcp dport 8443 accept
  }
  chain forward {
    type filter hook forward priority 0; policy drop;
  }
}
```

No afegeixis una regla general `accept` per a `wg0` o per a `10.8.0.0/24`. Ajusta `192.168.1.0/24` a la teva LAN i conserva les regles necessàries per a serveis locals.

### TLS i empremta SPKI

Fanel serveix l'API del túnel amb HTTPS. Genera un PKCS12 al directori `deploy/certs` (crea'l abans) amb la IP LAN i, opcionalment, noms/IP addicionals als SAN:

```sh
mkdir -p deploy/certs
cd deploy/certs
FANEL_TLS_KEYSTORE_PASSWORD='una-contrasenya-llarga' \
  ../../scripts/wg-gen-cert.sh 192.168.1.50 fanel.home.example
```

El certificat també inclou `10.8.0.1`; el script imprimeix `FANEL_WG_TLS_SPKI_SHA256`. Desa el password i l'empremta només al fitxer `.env` local, que no s'ha de pujar al repositori. L'app fixa l'empremta SPKI del servidor que s'ha incorporat al QR.

### Configuració d'entorn

Per a la topologia A en Docker, copia `deploy/.env.example` a `deploy/.env`, emplena els secrets i executa:

```sh
cd deploy
docker compose -f docker-compose.yml -f docker-compose.wireguard.yml up -d
```

| Variable | Valor o ús |
|---|---|
| `FANEL_WG_ENABLED` | L'override l'activa; per defecte, Fanel el manté desactivat. |
| `FANEL_WG_INTERFACE` | `wg0`. |
| `FANEL_WG_NETWORK` / `FANEL_WG_SERVER_ADDRESS` | `10.8.0.0/24` / `10.8.0.1`. |
| `FANEL_WG_ENDPOINT` | Host públic/DDNS i port UDP, p. ex. `vpn.example.net:51820`. |
| `FANEL_WG_LAN_BASE_URL` | HTTPS de Fanel a la LAN, inclòs el port. |
| `FANEL_WG_TUNNEL_BASE_URL` | `https://10.8.0.1:8443`. |
| `FANEL_WG_ALLOWED_IPS` | `10.8.0.0/24`; només el túnel, sense la LAN. |
| `FANEL_WG_KEEPALIVE` | Keepalive persistent del client, habitualment `25` segons. |
| `FANEL_WG_COMMAND` / `FANEL_WG_COMMAND_TIMEOUT` | Executable `wg` (`/usr/bin/wg`) i timeout d'ordres (`5s`). |
| `FANEL_WG_USE_PSK` / `FANEL_WG_PSK_KEY` | PSK activada per defecte; clau de xifrat AES-256 de 32 bytes en Base64. Genera-la amb `openssl rand -base64 32`. |
| `FANEL_WG_TLS_SPKI_SHA256` | Empremta Base64 de 32 bytes impresa pel script de certificat. |
| `FANEL_WG_SERVER_ID` | UUID estable per a aquesta instal·lació. |
| `FANEL_WG_PAIRING_TOKEN_TTL` | Caducitat dels codis, `10m` per defecte. |
| `FANEL_WG_RECONCILE_INTERVAL` | Interval de reconciliació, `5m` per defecte. |
| `FANEL_WG_REMOVE_UNKNOWN_PEERS` | `false` per defecte; activa la retirada de peers no registrats només si vols que Fanel imposi aquesta política. |
| `FANEL_WG_ENROLL_PRIVATE_ONLY` | Si és `true`, només permet l'emparellament des de xarxes privades/de confiança. |
| `FANEL_WG_TRUSTED_PROXIES` | IPs/CIDRs de proxies de confiança que poden establir l'IP del client via `X-Forwarded-For`. Deixa-ho buit si no hi ha proxy. |
| `FANEL_WG_ENROLL_RATE_LIMIT_PER_MINUTE` | Peticions d'emparellament per IP i minut, `5` per defecte. |
| `FANEL_WG_ENFORCE_TUNNEL_PEER` | Rebutja les peticions del túnel de peers que no siguin actius, activat per defecte. |
| `FANEL_WG_ALLOW_ADMIN_FROM_TUNNEL` | Permet endpoints d'administració des del túnel; desactivat per defecte. |
| `FANEL_TLS_KEYSTORE_PASSWORD` | Password del PKCS12; només local, no el comparteixis. |

L'override conserva un únic contenidor d'aplicació, usa `network_mode: host` perquè `wg` gestioni la interfície del host, i publica Postgres només a `127.0.0.1:5432`. El port 8080 de management queda a loopback HTTP per al health check existent; la API principal usa TLS a 8443. No exposis 8443 ni la base de dades a Internet.

### Execució del JAR al host

Si no fas servir Docker, executa el JAR amb un usuari dedicat i concedeix accés a WireGuard mitjançant un helper root-owned, no pas donant `sudo` genèric a l'aplicació. El helper `/usr/local/bin/fanel-wg` ha de validar els arguments i permetre només aquestes formes:

```text
wg set wg0 peer <public-key> allowed-ips <ip>/32 [preshared-key /dev/stdin]
wg set wg0 peer <public-key> remove
wg show wg0 dump
wg show wg0 public-key
```

Executa el helper amb una llista d'arguments, no amb shell, i valida estrictament la clau/IP abans d'invocar `wg`. Un fitxer sudoers root-owned (`/etc/sudoers.d/fanel-wg`, mode `440`) ha de permetre únicament el binari/helper i l'usuari del servei; configura `FANEL_WG_COMMAND` perquè apunti al helper. Prova cada forma com a usuari de servei i comprova que no pugui executar altres ordres.

### Emparellar i revocar

Un administrador global obre **Dispositius**, genera un codi de vinculació i escaneja el QR des del client. El token només s'utilitza una vegada i caduca; el QR inclou tant la URL LAN com la del túnel i l'empremta TLS. Després de la vinculació, l'usuari inicia sessió amb les credencials habituals. La pàgina mostra els peers registrats i permet revocar-los; també es pot eliminar el peer del dispositiu des del mateix client.

### Client Android

1. A la pantalla inicial, toca **Vincula amb el servidor (QR)** i escaneja el codi generat a **Dispositius**. Comprova l'identificador del servidor i l'adreça LAN abans de confirmar.
2. La vinculació inicial s'ha de fer connectat al Wi-Fi de casa perquè el client enrola la clau pública a la URL LAN. La clau privada es genera i es desa al dispositiu xifrada amb Android Keystore; no s'envia al servidor.
3. Accepta el permís de VPN d'Android. El túnel només inclou el paquet de Fanel i l'app manté les credencials d'inici de sessió habituals. Per a una instal·lació ja vinculada, la URL del servidor queda oculta; els usuaris sense vinculació continuen podent introduir-la manualment.
4. La configuració mostra si s'està fent servir la **xarxa local**, el **túnel remot**, si el dispositiu és **fora de línia** o si hi ha un error. En entrar a la LAN, el client prefereix HTTPS local i desactiva el túnel; fora de casa valida la identitat del servidor pel túnel abans d'enviar-hi peticions.
5. Per eliminar la vinculació, obre la configuració i toca **Desvincula el dispositiu**. El client intenta revocar el peer al servidor, atura la VPN i esborra la configuració xifrada. Tancar la sessió no elimina la vinculació.

Si apareix **Dispositiu revocat o servidor no accessible**, comprova el túnel, la connexió a Internet i que el peer continuï actiu al panell **Dispositius**; després toca **Torna a vincular**. Android només permet una VPN activa alhora. Si la vinculació inicial no arriba al servidor, connecta't al Wi-Fi de casa i torna a escanejar un codi vigent.

## Topologia B: túnel cap a la LAN (documentació)

En aquesta variant Fanel pot estar en un altre host de la LAN. A la configuració del client WireGuard, `AllowedIPs` inclou la xarxa del túnel **i** la IP LAN de Fanel com a `/32`, per exemple `10.8.0.0/24, 192.168.1.50/32`; `FANEL_WG_LAN_BASE_URL` és la URL LAN. El servidor WireGuard ha d'habilitar `ip_forward` i configurar una ruta de retorn a `10.8.0.0/24` al router/LAN o fer masquerade cap a la LAN. El tallafoc només ha de permetre el trànsit necessari cap a l'host Fanel. Aquesta topologia no és configurada automàticament pel compose d'exemple i requereix revisar l'encaminament i les regles de la xarxa local.

## Resolució de problemes

- Si el túnel connecta però HTTPS queda penjat, prova `MTU = 1280` a `wg0` i al client.
- En Android, deixa activa només una VPN alhora; una altra VPN pot impedir que WireGuard s'iniciï.
- No depenguis de NAT hairpin: dins de la LAN usa la URL LAN; des de fora usa l'endpoint públic del túnel.
- Comprova `sudo wg show wg0`, el reenviament UDP 51820, l'empremta SPKI, el SAN del certificat i els logs de Fanel.
- Si l'admin no pot veure o revocar peers, verifica que l'usuari tingui accés global. Si els peers es retiren inesperadament, revisa `FANEL_WG_REMOVE_UNKNOWN_PEERS`.
