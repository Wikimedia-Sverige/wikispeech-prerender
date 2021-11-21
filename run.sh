#!/bin/bash
export MAVEN_OPTS="-Xmx2g"
mvn clean install
mvn exec:java -Dinflux.username="" -Dinflux.password="" -Dexec.mainClass="se.wikimedia.wikispeech.prerender.WebApp"
