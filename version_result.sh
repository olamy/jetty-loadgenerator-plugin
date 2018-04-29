curl -XGET 'localhost:9200/loadresult/result/_search?pretty' -H 'Content-Type: application/json' -d'
{
    "size" : 100,
    "query": {
        "wildcard" : { "serverInfo.jettyVersion" : "9.1*" }
    }
    ,"sort":  { "timestamp": { "order": "desc" }}
}
'