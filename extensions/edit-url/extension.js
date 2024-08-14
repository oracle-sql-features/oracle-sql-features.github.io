'use strict'

const PARTIALS     = "partials"
const CATEGORIES   = "categories/pages"
const VERSIONS     = "versions/pages"
const INDEX        = "index.adoc"
const RE_CATEGORY1 = /modules\/categories\/pages\/(?<category>\w+)\/index.adoc/
const RE_VERSION1  = /modules\/versions\/pages\/(?<version>.+?)\/index.adoc/
const RE_CATEGORY2 = /modules\/categories\/pages\/\w+\/(?<version>.+?)\/index.adoc/
const RE_VERSION2  = /modules\/versions\/pages\/.+?\/(?<category>\w+)\/index.adoc/

module.exports.register = function () {
  this.on('contentAggregated', ({ contentAggregate }) => {
    contentAggregate.forEach(({ name, title, version, nav, files }) => {
      files.forEach((file) => {
        let path = file.src.path

        if (path.includes(PARTIALS)) {
          // skip
        } else if (file.src.basename == INDEX) {
          let match_c1 = path.match(RE_CATEGORY1)
          let match_c2 = path.match(RE_CATEGORY2)
          let match_v1 = path.match(RE_VERSION1)
          let match_v2 = path.match(RE_VERSION2)

          if (match_c1) {
            file.src.editUrl = file.src.origin.webUrl + "/blob/" +
                               file.src.origin.refname + "/features/_categories/" +
                               match_c1.groups.category + ".adoc"
          } else if(match_c2) {
            file.src.editUrl = file.src.origin.webUrl + "/blob/" +
                               file.src.origin.refname + "/features/_versions/" +
                               match_c2.groups.version + ".adoc"
          } else if(match_v2) {
            file.src.editUrl = file.src.origin.webUrl + "/blob/" +
                               file.src.origin.refname + "/features/_categories/" +
                               match_v2.groups.category + ".adoc"
          } else if (match_v1) {
            file.src.editUrl = file.src.origin.webUrl + "/blob/" +
                               file.src.origin.refname + "/features/_versions/" +
                               match_v1.groups.version + ".adoc"
          }
        } else if (path.includes(CATEGORIES) ||
                   path.includes(VERSIONS)) {
          file.src.editUrl = file.src.origin.webUrl + "/blob/" +
                             file.src.origin.refname + "/features/" +
                             file.src.basename
        }
      })
    })
  })
}