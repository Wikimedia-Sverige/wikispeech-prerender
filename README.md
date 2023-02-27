# Wikispeech-Prerender

Starting from scratch will 

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