# Offline Wallet Generator

## Application Description

Generate multiple offline wallets for several popular cryptocurrencies.

## Installation Instructions

Java is required to build and run this project.

### Build from Source

Clone this repo using the following command:

```
git clone git@github.com:ashelkovnykov/offline-wallet-generator.git
```

Inside the project root directory, run:

- Windows: `gradlew.bat build`
- Linux/MacOS: `./gradlew.sh build`

Alternatively, import the project as a Gradle project into Eclipse or IDEA, then build it.

### Release Version

TODO

## Running the Application

```sh
java -jar ./build/core/libs/core.jar
```

### Usage

```
-a, --account
      Specific account for which to compute address (see BIP32)
    -i, --address-index
      Specific address index for which to compute address (see BIP32)
    -g, --change
      Specify whether address is a change address (see BIP32)
    -c, --coin
      Crypto currency code of coin for which to generate wallet
      Possible Values: [BTC, LTC, DOGE, ETH, XRP, XLM, ALGO, AVAX]
    -m, --custom-mnemonic
      Custom mnemonic to use for generating wallets/addresses
    -e, --entropy
      Number of bits of entropy for randomly generated seed (must be 128-256 &
      multiple of 32)
      Default: 256
    -h, --help
      Show this usage details page
    -t, --hot
      Generate hot wallet for imminent use with a wallet application (will
      compute addresses for every supported coin type)
      Default: false
    -p, --mnemonic-password
      Password for mnemonic used to generate wallet master key
    -n, --num-addresses
      Number of addresses to generate
      Default: 1
    -d, --target-directory
      Output directory for generated wallet files
      Default: ${home}/Wallets/${date}.wal
```

## Acknowledgements

TODO
