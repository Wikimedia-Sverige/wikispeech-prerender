#!/usr/bin/env bash

if [ $# -eq 0 ]; then
  consumerUrl="https://sv.wikipedia.org/w"
else
  consumerUrl=$1
fi

curl -d "consumerUrl=${consumerUrl}&initialLastRecentChangesLimitInMinutes=0&mainPagePriority=10&maximumSynthesizedVoiceAgeInDays=30" -X POST http://localhost:8080/api/wiki
