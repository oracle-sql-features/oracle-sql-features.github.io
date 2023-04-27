#!/bin/bash

PWD=$(pwd)
FEATURES="${PWD}/features/"
CATEGORIES="${PWD}/docs/modules/categories"
VERSIONS="${PWD}/docs/modules/versions"
PARTIALS="${PWD}/docs/modules/features/partials"
GIT_COMMIT=""

echo "* xref:index.adoc[]" > "${CATEGORIES}/nav.adoc"
echo "* xref:index.adoc[]" > "${VERSIONS}/nav.adoc"
rm -rf "${PARTIALS}"
mkdir -p "${PARTIALS}"

for FEATURE in $(find "${FEATURES}" -name "*.adoc" -print)
do
    PAGE=$(basename "${FEATURE}")
    echo "➡️  Processing ${PAGE}"
    TARGET_PAGE="${PARTIALS}/${PAGE}"

    # bail out if target page already exists
    if [ -f "${TARGET_PAGE}" ];
    then
        echo "${PAGE} is not unique. One of these must be renamed:"
        find "${FEATURES}" -name "*.adoc" | grep "${PAGE}"
        exit 1
    fi

    cp "${FEATURE}" "${TARGET_PAGE}"
    DATABASE_CATEGORIES=$(grep ":database-category:" "${FEATURES}/${PAGE}")
    DATABASE_VERSION=$(grep ":database-version:" "${FEATURES}/${PAGE}" | awk '{print $2}')

    IFS=\  read -a CATEGORIES_ARRAY <<<"$DATABASE_CATEGORIES"
    for DATABASE_CATEGORY in "${CATEGORIES_ARRAY[@]:1}"; do
        DATABASE_CATEGORY=$(echo "$DATABASE_CATEGORY" | xargs echo -n )
        echo "DATABASE_CATEGORY = $DATABASE_CATEGORY"

        # create category page if it does not exist
        if [ ! -d "${CATEGORIES}/pages/${DATABASE_CATEGORY}" ];
        then
            mkdir "${CATEGORIES}/pages/${DATABASE_CATEGORY}"
            echo "= ${DATABASE_CATEGORY}\n" > "${CATEGORIES}/pages/${DATABASE_CATEGORY}/index.adoc"
            git add "${CATEGORIES}/pages/${DATABASE_CATEGORY}/index.adoc"
            GIT_COMMIT="true"
        fi

        # add category to list if not there already
        CATEGORY_LISTED=$(grep "xref:${DATABASE_CATEGORY}/index.adoc" "${CATEGORIES}/pages/index.adoc")
        if [ -z "${CATEGORY_LISTED}" ];
        then
            echo "* xref:${DATABASE_CATEGORY}/index.adoc[]" >> "${CATEGORIES}/pages/index.adoc"
        fi

        # create pages
        echo "include::features:partial\$${PAGE}[]" > "${CATEGORIES}/pages/${DATABASE_CATEGORY}/${PAGE}"

        # update navs
        CATEGORY_EXISTS=$(grep "${DATABASE_CATEGORY}/index.adoc" "${CATEGORIES}/nav.adoc")

        if [ -z "${CATEGORY_EXISTS}" ];
        then
            echo "** xref:${DATABASE_CATEGORY}/index.adoc[]" >> "${CATEGORIES}/nav.adoc"
        fi
        echo "*** xref:${DATABASE_CATEGORY}/${PAGE}[]" >> "${CATEGORIES}/nav.adoc"
    done

    echo "DATABASE_VERSION = $DATABASE_VERSION"
    # create version page if it does not exist
    if [ ! -d "${VERSIONS}/pages/${DATABASE_VERSION}" ];
    then
        mkdir "${VERSIONS}/pages/${DATABASE_VERSION}"
        echo "= ${DATABASE_VERSION}\n" > "${VERSIONS}/pages/${DATABASE_VERSION}/index.adoc"
        git add "${VERSIONS}/pages/${DATABASE_VERSION}/index.adoc"
        GIT_COMMIT="true"
    fi

    # add version to list if not there already
    VERSION_LISTED=$(grep "xref:${DATABASE_VERSION}/index.adoc" "${VERSIONS}/pages/index.adoc")
    if [ -z "${VERSION_LISTED}" ];
    then
        echo "* xref:${DATABASE_VERSION}/index.adoc[]" >> "${VERSIONS}/pages/index.adoc"
    fi

    # create pages
    echo "include::features:partial\$${PAGE}[]" > "${VERSIONS}/pages/${DATABASE_VERSION}/${PAGE}"

    # update navs
    VERSION_EXISTS=$(grep "${DATABASE_VERSION}/index.adoc" "${VERSIONS}/nav.adoc")

    if [ -z "${VERSION_EXISTS}" ];
    then
      echo "** xref:${DATABASE_VERSION}/index.adoc[]" >> "${VERSIONS}/nav.adoc"
    fi
    echo "*** xref:${DATABASE_VERSION}/${PAGE}[]" >> "${VERSIONS}/nav.adoc"
done

if [ -n "${GIT_COMMIT}" ];
then
    git config --global user.email "41898282+github-actions[bot]@users.noreply.github.com"
    git config --global user.name "GitHub Action"
    git push origin main
fi