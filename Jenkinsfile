#!groovy

// build both versions, retry test failures
buildPlugin(jenkinsVersions: [null, '2.150.1'],
            findbugs: [run:true, archive:true, unstableTotalAll: '0'],
            failFast: false)
