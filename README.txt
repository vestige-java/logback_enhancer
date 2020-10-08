Add main class if you recreate a module-info.class :

jar --main-class fr.gaellalire.vestige.logback_enhancer.JPMSLogbackEnhancer --update --file target/vestige.logback_enhancer*.jar; \
pushd src/main/resources/ && jar xf ../../../target/vestige.logback_enhancer*.jar module-info.class; popd

---------------

How to create logback.core 1.2.3.1

mvn clean source:jar javadoc:jar gpg:sign install -DcreateChecksum=true
# logback-core-1.2.3.1.jar will not be signed, so go to repository and launch
gpg -a --detach-sig -s logback-core-1.2.3.1.jar