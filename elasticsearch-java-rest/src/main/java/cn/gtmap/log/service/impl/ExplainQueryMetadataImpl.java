package cn.gtmap.log.service.impl;

import cn.gtmap.log.domain.query.QueryMetadata;
import cn.gtmap.log.service.ExplainQueryMetadata;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @author <a href="mailto:Administrator@gtmap.cn">Administrator</a>
 * @version 1.0, 2017/12/8
 * @description
 */
@Service
public class ExplainQueryMetadataImpl implements ExplainQueryMetadata {

    final RestClient client;

    public ExplainQueryMetadataImpl(RestClient client) {
        this.client = client;
    }

    @Override
    public String getSearchResponseByMetatdata(QueryMetadata queryMetadata) throws IOException {
        String endponit = queryMetadata.getEndpoint();

        QueryMetadata.AggregationMetaData aggregationMetaData = queryMetadata.getAggregationMetaData();

        List<QueryMetadata.MustQueryMetaData> queryMetaDatas = queryMetadata.getQueryMustMetaData();

        Map<String,Object> data = new HashMap<String,Object>();

        data.putAll(dealAggs(aggregationMetaData));
        data.putAll(dealQuery(queryMetaDatas));

        String str = JSON.toJSONString(data);

        Map<String, String> params = Collections.emptyMap();

        HttpEntity entity = new NStringEntity(str, ContentType.APPLICATION_JSON);

        Response response = client.performRequest("POST", endponit, params, entity);
        HttpEntity entity1 = response.getEntity();
        return convertStreamToString(entity1.getContent());
    }

    public String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "/n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }

    private Map<String,Object> dealAggs(QueryMetadata.AggregationMetaData aggregationMetaData) {
        boolean flag = true;

        Map<String,Object> aggr = new HashMap<String,Object>();

        Map<String,Object> unit = new HashMap<String,Object>();

        do {
            Map<String,Object> newValue = new HashMap<String,Object>();
            Map<String,Object> aggr2 = new HashMap<String,Object>();
            Map<String,String> aggr3 = new HashMap<String,String>();

            if(flag) {
                flag = false;
                aggr.put("aggs", newValue);
                newValue.put("count", aggr2);
                aggr2.put("terms", aggr3);
                aggr3.put("field", aggregationMetaData.getFieldName());
            } else {
                unit.put("aggrs", newValue);
                newValue.put("count", aggr2);
                aggr2.put("terms", aggr3);
                aggr3.put("field", aggregationMetaData.getFieldName());
            }
            unit = newValue;
            aggregationMetaData = aggregationMetaData.getAggregationMetaData();

        } while (aggregationMetaData != null);
        return aggr;
    }

    private Map<String,Object> dealQuery (List<QueryMetadata.MustQueryMetaData> queryMetaDatas) {
        Map<String,Object> bool = new HashMap<String,Object>();
        Map<String,Object>  must = new HashMap<String,Object>();
        List<Object> mustObject = new ArrayList<Object>();
        Map<String,Object> query = new HashMap<String,Object>();
        query.put("query", bool);
        bool.put("bool", must);
        must.put("must", mustObject);

        for(QueryMetadata.MustQueryMetaData queryDetail : queryMetaDatas) {
            Map<String,Object> contentType = new HashMap<String,Object>();
            Map<String,Object> contentFileName = new HashMap<String,Object>();
            contentType.put(queryDetail.getFilterDo(), contentFileName);

            if(queryDetail.getSymbol().get(0).equals("equals")){
                contentFileName.put(queryDetail.getFiledName(), queryDetail.getFiledValue().get(0));
            }else if(queryDetail.getSymbol().size() > 1) {
                List<String> symbol = queryDetail.getSymbol();
                List<String> symbolValue = queryDetail.getFiledValue();
                Map<String,Object> range = new HashMap<String,Object>();
                Map<String,Object> rangeContent = new HashMap<String,Object>();
                contentType.put("range", range);
                range.put(queryDetail.getFiledName(), rangeContent);
                for(int i = 0; i < symbol.size(); i++) {
                    rangeContent.put(symbol.get(i), symbolValue.get(i));
                }

            }
            mustObject.add(contentType);
        }
        return query;
    }
}