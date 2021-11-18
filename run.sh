#!/bin/bash

mvn clean install
mvn exec:java -Dexec.mainClass="se.wikimedia.wikispeech.prerender.WebApp"
