#!/bin/bash
export MAVEN_OPTS="-Xmx2g"
mvn clean install
mvn exec:java -Dexec.mainClass="se.wikimedia.wikispeech.prerender.WebApp"
