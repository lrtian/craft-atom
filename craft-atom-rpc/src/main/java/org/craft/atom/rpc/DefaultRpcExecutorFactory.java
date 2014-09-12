package org.craft.atom.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.craft.atom.protocol.rpc.model.RpcMessage;
import org.craft.atom.protocol.rpc.model.RpcMethod;
import org.craft.atom.rpc.api.RpcParameter;
import org.craft.atom.rpc.spi.RpcExecutorFactory;
import org.craft.atom.util.NamedThreadFactory;

/**
 * @author mindwind
 * @version 1.0, Aug 12, 2014
 */
public class DefaultRpcExecutorFactory implements RpcExecutorFactory {

	
	private Map<String, ExecutorService> pool     = new ConcurrentHashMap<String, ExecutorService>();
	private RpcRegistry                  registry = RpcRegistry.getInstance()                       ;
	
	
	@Override
	public ExecutorService getExecutor(RpcMessage msg) {
		String    rpcId        = msg.getBody().getRpcId(); 
		Class<?>  rpcInterface = msg.getBody().getRpcInterface();
		RpcMethod rpcMethod    = msg.getBody().getRpcMethod();
		String key = registry.key(rpcId, rpcInterface, rpcMethod);
		return getExecutor(key);
	}
	
	private ExecutorService getExecutor(String key) {
		ExecutorService es = pool.get(key);
		if (es == null) {
			synchronized (this) {
				if (es == null) {
					RpcEntry     entry     = registry.lookup(key);
					RpcParameter parameter = entry.getRpcParameter();
					int          threads   = parameter.getRpcThreads() == 0 ? 1 : parameter.getRpcThreads();
					int          queues    = parameter.getRpcQueues()  == 0 ? 1 : parameter.getRpcQueues() ;
					ThreadPoolExecutor tpe = new RpcThreadPoolExecutor(threads, threads, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(queues), new NamedThreadFactory("craft-atom-rpc"));
					tpe.allowCoreThreadTimeOut(true);
					es = tpe;
					pool.put(key, tpe);
				}
			}
		}
		return es;
	}

}
