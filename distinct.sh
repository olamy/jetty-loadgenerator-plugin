curl -s -XGET 'localhost:9200/loadresult/result/_search?pretty' -H 'Content-Type: application/json'  -d'
{
  "size": 0,
  "aggs": {
    "version": {
      "terms": {
        "field": "serverInfo.jettyVersion.keyword"
      }
    }
  }
}'