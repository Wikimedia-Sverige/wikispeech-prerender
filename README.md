# Wikispeech-Prerender

## Installing and running

### Requirements

```
apt-get install maven openjdk-11-jdk
```

This service keeps the state in RAM using [system prevalence pattern](https://en.wikipedia.org/wiki/System_prevalence) (rather than using a database).
The service has been coded in a way that it hopefully shouldn't grow the heap larger than 1GB,
but there is no guarantee that this limit won't be exceeded. If you have a lot of RAM on the machine,
consider increasing the -Xmx value in ```run.sh```.

### For the first time
```
./run.sh (start service on port 9090)
./register-wiki.sh [consumer url, defaults to svwp]
```


### When installed
```
./run.sh
```

### Clear state and start from scratch

The system state is store in directory ```prevalence```. To start from scratch,
simply stop service, delete the directory ```prevalence``` start the service again.
You will at this point once again have to register the wikis you want to pre-render.

```
rm -rf prevalence
./run.sh
./register-wiki.sh
```


## What this service does

* It finds pages to segment and synthesize by
  * Polling main page metadata once every minute to detect updates. 
    (This could be improved by listening at recent changes, but that requires consideration).
  * Harvesting wiki links from main page.   
  * Polling for updated pages from recent changes.  

All you need to do is to register the "consumer URL" of a wiki (eg ```https://sv.wikipedia.org/w```), and this service will figure everything else out: languages, voices, etc.

The selected order to synthesize segments is evaluated from priority settings:

* The further down a segment occurs on a page (the greater the segment index),
  the less priority the segment receives. This is a minuscule change of priority.
* Pages linked from main page get a multiplication factor of 5 to all segments.
* Main page get a multiplication factor of 10 on all segments. 

Basically this means the following order when synthesizing:
1. All segements in the main page.
2. The first segment in pages linked from the current main page.
3. The second segment in pages linked from the current main page.
4. ... until all segments in all pages links from the current main page is synthesized.
5. The first segment in pages found in recent changes.
6. The second segment in pages found in recent changes.
7. ... until all segments in all pages found in recent changes has been synthesized.

As the number of candidates to be synthesized can grow very large in a rather short time,
a flushing mechanism kicks in when there are more than 100,000 candidates in the queue, 
removing those with the lowest priority and retains the top 100,000.

## TODO

* Add feature in Wikispeech to not send audio response on synthesis, in order to minimize network data.
* Flush out pages that have not been updated for x days 
* Make the 100000 value in a property file or something (hard coded in SynthesizeService.java)
* Make default priorities configurable in properties file (hard coded 5F in MainPagePrioritizer.java)
* Report state of candidate, flushing, etc to influxdb.



## REST

Most REST calls are exist for debug and development reasons.
As a user of this service, all you need is ```POST /api/wiki```

### POST /api/wiki
* consumerUrl: Wiki to be monitored for pre rendered.
* initialLastRecentChangesLimitInMinutes: (default: 60) Number of hours of initial recent changes backlog to be processed.
* mainPagePriority: (default: 10) Base priority of segments on Wiki main page.
* maximumSynthesizedVoiceAgeInDays: (default: 30) Number of days before attempting to re-synthesizing segments on this Wiki. 

If initialLastRecentChangesLimitInHours is set to 0, then only new recent changes will be processed. 

Example: ```POST http://host:port/api/wiki?consumerUrl=https://sv.wikipedia.org/w```

### GET /api/synthesis/queue/candidates
* limit: (default 100) Maximum number of results 
* startOffset: (default 0) Start offset for pagination

Queue of Wiki page segments in line to be synthesized using a specific language and voice.

### DELETE /api/synthesis/queue

Clears queue of Wiki page segments in line to be synthesized.

### GET /api/synthesis/errors
* limit: (default 100) Maximum number of results
* startOffset: (default 0) Start offset for pagination

A list of errors that have occurred during synthesis of Wiki page segments.

### GET /api/page
* consumerUrl: Wiki
* title: Wikie page title

Example: ```GET http://host:port/api/page?consumerUrl=https://sv.wikipedia.org/w&title=Portal:Huvudsida```

Displays status and statistics about a given Wiki page.
* Priority
* Language
* Revision at segmentation
* Timestamp segmented
* Segments
* Voices synthesized
* Timestamp synthesized
* Synthesized revision
* etc