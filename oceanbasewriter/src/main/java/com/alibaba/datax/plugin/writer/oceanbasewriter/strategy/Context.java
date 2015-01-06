package com.alibaba.datax.plugin.writer.oceanbasewriter.strategy;

import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.plugin.TaskPluginCollector;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.writer.oceanbasewriter.Key;
import com.alibaba.datax.plugin.writer.oceanbasewriter.OceanbaseErrorCode;
import com.alibaba.datax.plugin.writer.oceanbasewriter.utils.OBDataSource;
import com.alibaba.datax.plugin.writer.oceanbasewriter.utils.ResultSetHandler;

import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class Context {
	
	public final RecordReceiver recordReceiver;
	private final Configuration configuration;
    private final TaskPluginCollector taskPluginCollector;
	public static volatile boolean permit = true;
	public static final long daemon_check_interval = 30 * 1000;
	
	public Context(Configuration configuration, RecordReceiver recordReceiver, TaskPluginCollector taskPluginCollector) {
		this.recordReceiver = recordReceiver;
		this.configuration = configuration;
        this.taskPluginCollector = taskPluginCollector;
	}

	public String url(){
		return this.configuration.getString(Key.CONFIG_URL);
	}
	
	public boolean useDsl(){
		return !"".equals(this.configuration.getString(Key.ADVANCE, ""));
	}
	
	public String table(){
		return this.configuration.getString(Key.TABLE);
	}
	
	public int batch(){
		return this.configuration.getInt(Key.BATCH_SIZE, 1000);
	}
	
	public List<String> normal(){
		return this.configuration.getList(Key.COLUMNS, String.class);
	}
	
	public List<String> dsl(){
		return this.configuration.getList(Key.ADVANCE,String.class);
	}

    public String writeMode(){
        return this.configuration.getString(Key.WRITE_MODE);
    }
	
	public Map<String, String> columnType() {
		ResultSetHandler<Map<String, String>> handler = new ResultSetHandler<Map<String, String>>() {
			@Override
			public Map<String, String> callback(ResultSet result)
					throws Exception {
				Map<String, String> map = new LinkedHashMap<String, String>();
				while (result.next()) {
					String name = result.getString("field").toLowerCase();
					String type = result.getString("type").toLowerCase();
					if(type.equalsIgnoreCase("createtime") || type.equalsIgnoreCase("modifytime")){
						type = "timestamp";
					}
					map.put(name, type);
				}
				return map;
			}
		};
		try {
			return OBDataSource.executeQuery(url(), String.format("desc %s", table()), handler);
		} catch (Exception e) {
			throw DataXException.asDataXException(OceanbaseErrorCode.DESC,e);
		}
	}

    private static final AtomicLong failNumber = new AtomicLong(0);

	public void reportFail(Record record,Exception e){
        failNumber.incrementAndGet();
        this.taskPluginCollector.collectDirtyRecord(record,e);
	}

    public long failNumber(){
        return failNumber.longValue();
    }
	
	public long activeMemPercent(){
		return this.configuration.getInt(Key.ACTIVE_MEM_PERCENT, 60);
	}

}