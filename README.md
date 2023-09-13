# Oracle SQL Features

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md)

## Building the website locally

It is encouraged to build the website locally first before pushing to GitHub to
detect rendering issues or typos before the changes go live.

### Dependencies

1. Install Antora as explained at https://docs.antora.org/antora/latest/install-and-run-quickstart/
1. Install Java 11 or greater https://www.oracle.com/java/technologies/downloads/

### Build

1. Run the `generate.sh` script
    ```shell
    $ sh generate.sh
    ```
1. Inspect the generated site at `$PWD/build/site/index.html`
