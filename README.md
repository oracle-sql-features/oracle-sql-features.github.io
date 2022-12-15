# Oracle SQL Features

## Building

1. Install Antora as explained at https://docs.antora.org/antora/latest/install-and-run-quickstart/
2. Refresh navigation files by invoking
```shell
$ sh .github/scripts/generate-navigation.sh
```
3. Generate the site using the `local-playbook.yml` file
```sh
$ antora local-playbook.yml
```
4. Inspect the generated site at `build/site/index.html`
