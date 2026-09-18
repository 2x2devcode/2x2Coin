# User manual

## First use

1. Install the **2x2 Wallet** APK
2. Tap **Create wallet**
3. Set a strong password (minimum 8 characters)
4. Store the WIF backup somewhere safe

## Receive 2x2

1. Open the **Receive** tab
2. Copy the address or show the **QR code**
3. Send 2x2 to that address on the 2x2 network

## Send 2x2

1. Open **Send**
2. Enter the destination address or tap **Scan QR**
3. Enter the amount and confirm — the transaction is signed on the phone and broadcast through the official API

## Biometrics

1. Unlock the wallet with the password
2. In **Settings**, enable **Biometric unlock**
3. Confirm the password when asked
4. Later launches can use the **Biometrics** button on the login screen

The session locks automatically when the app goes to the background.

## Restore wallet (WIF)

1. On the **Receive** tab, tap **Restore WIF**
2. Paste the private key exported earlier
3. The wallet uses that account for the current session

## Multiple addresses

1. Open **Wallets**
2. Each row shows the label, address, and balance
3. Tap a row to make that address **active** (Receive, Send, and WIF export use only the active address)
4. Tap **Generate new address** to add another key — this does **not** switch away from the funded address

The Home screen shows the **sum of every address** in the wallet, plus a per-address breakdown. If coins sit on an inactive address, Home tells you to activate it in Wallets.

## Backup

1. In **Wallets**, tap **Export WIF of the active account**
2. Store the WIF offline
3. Never share the WIF

## Security

- The wallet is **non-custodial**
- The 2x2 team does **not** have access to your keys
- Without internet you can still see addresses already generated after unlock
- Losing the password and the WIF means permanent loss of funds

## Network

- Official API: `https://server.2x2coin.com`
- Explorer fallback: `https://serverexplorer.2x2coin.com`

App developer (HTTPS + curl, no VPS): [APP_API.md](APP_API.md).
Server operator: [SERVER.md](SERVER.md).

If the API is down, the balance may lag until the fallback responds.

If Home shows `0` while a block explorer shows coins, confirm the explorer address is listed under **Wallets**. The API is per-address: `https://server.2x2coin.com/api/address/<that-address>/balance`.
