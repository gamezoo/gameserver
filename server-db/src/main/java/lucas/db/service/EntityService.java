package lucas.db.service;

import lucas.common.util.ApplicationContextUtils;
import lucas.db.annnotation.DbMapper;
import lucas.db.annnotation.CacheOperation;
import lucas.db.entity.AbstractEntity;
import lucas.db.entity.IEntity;
import lucas.db.enums.OperationEnum;
import lucas.db.mapper.IDBMapper;
import lucas.db.service.proxy.EntityProxy;
import lucas.db.service.proxy.EntityProxyFactory;
import org.apache.commons.collections.MapUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lushengkao vip8
 * 2018/11/15 15:18
 */
public class EntityService<T extends AbstractEntity> implements IEntityService<T> {

    private Class<T> entityClass;

    private SqlSessionTemplate sqlSessionTemplate;

    public Class<T> getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public SqlSessionTemplate getSqlSessionTemplate() {
        return sqlSessionTemplate;
    }

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    public EntityService() {
    }

    @Override
    @CacheOperation(OperationEnum.insert)
    public long insertEntity(T entity) {
        IDBMapper<T> mapper = getEntityMapper(entity);
        return mapper.insertEntity(entity);
    }

    @Override
    @CacheOperation(OperationEnum.query)
    public T getEntity(Serializable id) {
        try {
            IDBMapper<T> mapper = getEntityMapper(entityClass);
            T entity = mapper.getEntity(id);
            ApplicationContext context = ApplicationContextUtils.getApplicationContext();
            EntityProxyFactory factory = context.getBean(EntityProxyFactory.class);
            return factory.createProxyEntity(entity);
        } catch (Exception e) {
            e.printStackTrace();
        }
       throw new IllegalStateException("不存在的数据库id--" + entityClass.getSimpleName() + " :" + id);
    }

    @Override
    @CacheOperation(OperationEnum.queryList)
    public List<T> getEntityList(Serializable key) {
        return getEntityMapper(entityClass).getEntityList(key);
    }

    @Override
    @CacheOperation(OperationEnum.update)
    @SuppressWarnings("unchecked")
    public void updateEntity(T entity) {
        EntityProxy proxy = entity.getProxy();
        if (proxy == null) {
            return;
        }
        Map changeParamMap = proxy.getChangeParamMap();
        if (MapUtils.isEmpty(changeParamMap)) {
            return;
        }
        Map data = new HashMap<>(changeParamMap);
        IDBMapper<T> mapper = getEntityMapper(entity);
        mapper.updateEntityByMap(data);
    }

    @Override
    @CacheOperation(OperationEnum.delete)
    public void deleteEntity(T entity) {
        IDBMapper<T> mapper = getEntityMapper(entity);
        mapper.deleteEntity(entity);
    }

    @Override
    @CacheOperation(OperationEnum.insertBatch)
    public List<Long> insertEntityBatch(List<T> entityList) {
        //TODO
        return null;
    }

    @Override
    @CacheOperation(OperationEnum.updateBatch)
    public void updateEntityBatch(List<T> entityList) {
        //TODO
    }

    @Override
    @CacheOperation(OperationEnum.deleteBatch)
    public void deleteEntityBatch(List<T> entityList) {
        //TODO
    }

    @Override
    public long getShardingId(T entity) {
        return 0;
    }

    @SuppressWarnings("unchecked")
    private IDBMapper<T> getEntityMapper(T entity) {
        Class<? extends IEntity> entityClass = entity.getClass();
        DbMapper dbMapper = entityClass.getAnnotation(DbMapper.class);
        Class<? extends IDBMapper> mapperClazz = dbMapper.mapper();
        return sqlSessionTemplate.getMapper(mapperClazz);
    }

    @SuppressWarnings("unchecked")
    private IDBMapper<T> getEntityMapper(Class<? extends T> entityClazz) {
        DbMapper dbMapper = entityClazz.getAnnotation(DbMapper.class);
        Class<? extends IDBMapper> mapperClazz = dbMapper.mapper();
        return sqlSessionTemplate.getMapper(mapperClazz);
    }
}
