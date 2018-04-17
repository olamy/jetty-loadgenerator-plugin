curl -s -XPUT 'localhost:9200/loadresult/_mapping/result?pretty&update_all_types' -H 'Content-Type: application/json'  -d'
{
  "properties": {
    "serverInfo.jettyVersion": {
      "type":     "text",
      "fielddata": true
    }
  }
}
'
