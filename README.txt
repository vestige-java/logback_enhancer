Add main class if you recreate a module-info.class :

jar --main-class fr.gaellalire.vestige.logback_enhancer.JPMSLogbackEnhancer --update --file target/vestige.logback_enhancer*.jar; \
pushd src/main/resources/ && jar xf ../../../target/vestige.logback_enhancer*.jar module-info.class; popd
