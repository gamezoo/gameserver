package lucas.common.eventbus;

import lucas.common.log.Loggers;
import lucas.common.util.ThreadPoolUtil;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

/**
 * @author lushengkao vip8
 * Dispatches events to listeners, and provides ways for listeners to register themselves.
 *
 * 2018/10/26 15:10
 */
@Component
public class EventBus {

    private Logger logger = Loggers.EVENT_BUS;

    private final static int THREAD_SIZE = 3;

    //所有监听者 event--监听者列表
    private ConcurrentHashMap<Class, List<Subscriber>> subscribersMap = new ConcurrentHashMap<Class, List<Subscriber>>();

    private ExecutorService[] executorServices = ThreadPoolUtil.createSingleExecutorServiceArray("event-bus",THREAD_SIZE);

    //注册监听者
    Object registerSubscriber(Object bean) {
        Class<?> clazz = bean.getClass();
        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method method : declaredMethods) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation != null) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1) {
                    throw new IllegalArgumentException("监听者注册失败，应只有一个参数，类名:" + clazz.getName());
                }
                Class<?> parameterType = parameterTypes[0];
                if (!parameterType.isAssignableFrom(IEvent.class)) {
                    throw new IllegalArgumentException("监听者注册失败，必须继承IEvent接口，类名:" + clazz.getName());
                }
                registerSubscriber0(bean,method, parameterType);
            }
        }
        return bean;
    }

    private void registerSubscriber0(Object bean, Method method, Class<?> event) {
        Subscriber subscriber = new Subscriber(bean,method);
        CopyOnWriteArrayList<Subscriber> value = new CopyOnWriteArrayList<Subscriber>();
        List<Subscriber> subscribers = subscribersMap.putIfAbsent(event, value);
        if (subscribers != null) {
            subscribers.add(subscriber);
        }else {
            value.add(subscriber);
        }
    }

    public void syncSubmit(IEvent event) {
        List<Subscriber> subscribers = subscribersMap.get(event.getClass());
        if (CollectionUtils.isEmpty(subscribers)) {
            return;
        }
        update(event, subscribers);
    }

    private void update(IEvent event, List<Subscriber> subscribers) {
        for (Subscriber subscriber : subscribers) {
            Method method = subscriber.getMethod();
            Object bean = subscriber.getBean();
            try {
                method.invoke(bean,event);
            } catch (Exception e) {
                logger.info(e.getMessage());
            }
        }
    }

    public void asyncSubmit(final IEvent event) {
        final List<Subscriber> subscribers = subscribersMap.get(event.getClass());
        if (CollectionUtils.isEmpty(subscribers)) {
            return;
        }
        ExecutorService executorService = selectExecutorService(event);
        executorService.submit(new Runnable() {
            public void run() {
                update(event,subscribers);
            }
        });
    }

    private ExecutorService selectExecutorService(IEvent event) {
        return executorServices[(int) (event.getId() % THREAD_SIZE)];
    }
}
