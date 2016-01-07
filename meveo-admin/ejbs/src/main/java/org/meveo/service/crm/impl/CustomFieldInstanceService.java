package org.meveo.service.crm.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.ProviderNotAllowedException;
import org.meveo.cache.CustomFieldsCacheContainerProvider;
import org.meveo.commons.utils.ParamBean;
import org.meveo.event.CFEndPeriodEvent;
import org.meveo.model.BaseEntity;
import org.meveo.model.BusinessEntity;
import org.meveo.model.ICustomFieldEntity;
import org.meveo.model.IEntity;
import org.meveo.model.IProvider;
import org.meveo.model.admin.User;
import org.meveo.model.crm.CustomFieldInstance;
import org.meveo.model.crm.CustomFieldMapKeyEnum;
import org.meveo.model.crm.CustomFieldStorageTypeEnum;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.CustomFieldTypeEnum;
import org.meveo.model.crm.CustomFieldValue;
import org.meveo.model.crm.Provider;
import org.meveo.service.base.PersistenceService;
import org.meveo.util.PersistenceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class CustomFieldInstanceService extends PersistenceService<CustomFieldInstance> {

    public static String MATRIX_VALUE_SEPARATOR = "|";
    public static String RON_VALUE_SEPARATOR = "<";

    @Inject
    private CustomFieldTemplateService cfTemplateService;

    @Inject
    private CustomFieldsCacheContainerProvider customFieldsCacheContainerProvider;

    @Inject
    private Event<CFEndPeriodEvent> cFEndPeriodEvent;

    @Resource
    private TimerService timerService;

    private ParamBean paramBean = ParamBean.getInstance();

    @Override
    public void create(CustomFieldInstance cfi) throws BusinessException {
        throw new RuntimeException(
            "CustomFieldInstanceService.create(CustomFieldInstance cfi) method not supported. Should use CustomFieldInstanceService.create(CustomFieldInstance cfi, ICustomFieldEntity entity) method instead");
    }

    public void create(CustomFieldInstance cfi, ICustomFieldEntity entity, User creator, Provider provider) throws BusinessException {
        super.create(cfi, creator, provider);
        customFieldsCacheContainerProvider.addUpdateCustomFieldInCache(entity, cfi);

        triggerEndPeriodEvent(cfi);
    }

    @Override
    public CustomFieldInstance update(CustomFieldInstance e) {
        throw new RuntimeException(
            "CustomFieldInstanceService.update(CustomFieldInstance cfi) method not supported. Should use CustomFieldInstanceService.update(CustomFieldInstance cfi, ICustomFieldEntity entity) method instead");
    }

    public CustomFieldInstance update(CustomFieldInstance cfi, ICustomFieldEntity entity, User updater) {
        cfi = super.update(cfi, updater);
        customFieldsCacheContainerProvider.addUpdateCustomFieldInCache(entity, cfi);

        triggerEndPeriodEvent(cfi);

        return cfi;
    }

    // @Override
    // public void remove(CustomFieldInstance e) {
    // throw new RuntimeException(
    // "CustomFieldInstanceService.remove(CustomFieldInstance cfi) method not supported. Should use CustomFieldInstanceService.remove(CustomFieldInstance cfi, ICustomFieldEntity entity) method instead");
    // }

    public void remove(CustomFieldInstance cfi, ICustomFieldEntity entity) {
        customFieldsCacheContainerProvider.removeCustomFieldFromCache(entity, cfi);
        super.remove(cfi.getId());
    }

    /**
     * Get a list of custom field instances to populate a cache
     * 
     * @return A list of custom field instances
     */
    public List<CustomFieldInstance> getCFIForCache() {

        TypedQuery<CustomFieldInstance> query = getEntityManager().createNamedQuery("CustomFieldInstance.getCfiForCache", CustomFieldInstance.class);
        return query.getResultList();
    }

    // /**
    // * Convert BusinessEntityWrapper to an entity by doing a lookup in DB
    // *
    // * @param businessEntityWrapper Business entity information
    // * @return A BusinessEntity object
    // */
    // @SuppressWarnings("unchecked")
    // public BusinessEntity convertToBusinessEntityFromCfV(EntityReferenceWrapper businessEntityWrapper, Provider provider) {
    // if (businessEntityWrapper == null) {
    // return null;
    // }
    // Query query = getEntityManager().createQuery("select e from " + businessEntityWrapper.getClassname() + " e where e.code=:code and e.provider=:provider");
    // query.setParameter("code", businessEntityWrapper.getCode());
    // query.setParameter("provider", provider);
    // List<BusinessEntity> entities = query.getResultList();
    // if (entities.size() > 0) {
    // return entities.get(0);
    // } else {
    // return null;
    // }
    // }

    @SuppressWarnings("unchecked")
    public List<BusinessEntity> findBusinessEntityForCFVByCode(String className, String wildcode, Provider provider) {
        Query query = getEntityManager().createQuery("select e from " + className + " e where lower(e.code) like :code and e.provider=:provider");
        query.setParameter("code", "%" + wildcode.toLowerCase() + "%");
        query.setParameter("provider", provider);
        List<BusinessEntity> entities = query.getResultList();
        return entities;
    }

    public Object getOrCreateCFValueFromParamValue(String code, String defaultParamBeanValue, ICustomFieldEntity entity, boolean saveInCFIfNotExist, User currentUser)
            throws BusinessException {

        Object value = getCFValue(entity, code, currentUser);
        if (value != null) {
            return value;
        }

        // If value is not found, create a new Custom field with a value taken from configuration parameters
        value = ParamBean.getInstance().getProperty(code, defaultParamBeanValue);
        if (value == null) {
            return null;
        }
        try {
            // If no template found - create it first
            CustomFieldTemplate cft = cfTemplateService.findByCodeAndAppliesTo(code, entity, currentUser.getProvider());
            if (cft == null) {
                cft = new CustomFieldTemplate();
                cft.setCode(code);
                cft.setAppliesTo(CustomFieldTemplateService.calculateAppliesToValue(entity));
                cft.setActive(true);
                cft.setDescription(code);
                cft.setFieldType(CustomFieldTypeEnum.STRING);
                cft.setDefaultValue(value.toString());
                cft.setValueRequired(false);
                cfTemplateService.create(cft, currentUser, currentUser.getProvider());
            }

            CustomFieldInstance cfi = CustomFieldInstance.fromTemplate(cft, entity);

            if (saveInCFIfNotExist) {
                create(cfi, entity, currentUser, currentUser.getProvider());
            }
        } catch (CustomFieldException e) {
            log.error("Can not determine applicable CFT type for entity of {} class. Value from propeties file will NOT be saved as customfield", entity.getClass().getSimpleName());
        }
        return value;
    }

    /**
     * Get a custom field value for a given entity
     * 
     * @param entity Entity
     * @param code Custom field code
     * @return Custom field value
     */
    public Object getCFValue(ICustomFieldEntity entity, String code, User currentUser) {

        boolean useCache = Boolean.parseBoolean(paramBean.getProperty("cache.cacheCFI", "true"));

        CustomFieldTemplate cft = customFieldsCacheContainerProvider.getCustomFieldTemplate(code, entity);
        if (cft == null) {
            //log.trace("No CFT found {}/{}", entity, code);
            return null;
        }

        if (cft.isVersionable()) {
            log.warn("Trying to access a versionable custom field {}/{} value with no provided date. Null will be returned", entity.getClass().getSimpleName(), code);
            return null;
        }

        Object value = null;

        // Try cache if applicable
        if (cft.isCacheValue() && useCache) {
            value = customFieldsCacheContainerProvider.getValue(entity, code);

            // Or retrieve directly from DB
        } else {
            TypedQuery<CustomFieldValue> query = getEntityManager().createNamedQuery("CustomFieldInstance.getCfiValueByCode", CustomFieldValue.class);
            query.setParameter("appliesToEntity", entity.getUuid());
            query.setParameter("code", code);
            query.setParameter("provider", getProvider(entity));

            List<CustomFieldValue> cfvs = query.getResultList();
            if (!cfvs.isEmpty()) {
                CustomFieldValue cfv = cfvs.get(0);

                cfv.deserializeValue();
                value = cfv.getValue();
            }
        }

        // Create such CF with default value if one is specified on CFT
        if (value == null && cft.getDefaultValue() != null && currentUser != null) {
            value = cft.getDefaultValueConverted();
            try {
                setCFValue(entity, code, value, currentUser);
            } catch (BusinessException e) {
                log.error("Failed to set a default Custom field value {}/{}", entity.getClass().getSimpleName(), code, e);
            }
        }

        return value;
    }

    /**
     * Get a custom field value for a given entity and a date
     * 
     * @param entity Entity
     * @param code Custom field code
     * @param date Date
     * @return Custom field value
     */
    public Object getCFValue(ICustomFieldEntity entity, String code, Date date, User currentUser) {

        boolean useCache = Boolean.parseBoolean(paramBean.getProperty("cache.cacheCFI", "true"));

        // If field is not versionable - get the value without the date
        CustomFieldTemplate cft = customFieldsCacheContainerProvider.getCustomFieldTemplate(code, entity);
        if (cft == null) {
            log.trace("No CFT found {}/{}", entity, code);
            return null;
        }
        if (!cft.isVersionable()) {
            return getCFValue(entity, code, currentUser);
        }

        Object value = null;

        // Check cache first TODO need to check if date falls within cacheable period date timeframe
        if (cft.isCacheValue() && useCache) {
            value = customFieldsCacheContainerProvider.getValue(entity, code, date);

        } else {
            TypedQuery<CustomFieldValue> query = getEntityManager().createNamedQuery("CustomFieldInstance.getCfiValueByCodeAndDate", CustomFieldValue.class);
            query.setParameter("appliesToEntity", entity.getUuid());
            query.setParameter("code", code);
            query.setParameter("provider", getProvider(entity));
            query.setParameter("date", date);

            List<CustomFieldValue> cfvs = query.getResultList();
            if (!cfvs.isEmpty()) {
                CustomFieldValue cfv = cfvs.get(0);
                cfv.deserializeValue();
                value = cfv.getValue();
            }
        }

        // Create such CF with default value if one is specified on CFT and field is versioned by a calendar
        if (value == null && cft.getDefaultValue() != null && cft.getCalendar() != null && currentUser != null) {
            value = cft.getDefaultValueConverted();
            try {
                setCFValue(entity, code, value, date, currentUser);
            } catch (BusinessException e) {
                log.error("Failed to set a default Custom field value {}/{}", entity.getClass().getSimpleName(), code, e);
            }
        }

        return value;
    }

    /**
     * Get custom field values of an entity as JSON string
     * 
     * @param entity Entity
     * @return JSON format string
     */
    public String getCFValuesAsJson(ICustomFieldEntity entity) {

        String result = "";
        String sep = "";

        Map<String, List<CustomFieldInstance>> customFieldsMap = getCustomFieldInstances(entity);

        for (List<CustomFieldInstance> customFields : customFieldsMap.values()) {
            for (CustomFieldInstance cf : customFields) {
                result += sep + cf.toJson();
                sep = ";";
            }
        }

        return result;
    }

    /**
     * Set a Custom field value on an entity
     * 
     * @param entity Entity
     * @param code Custom field value code
     * @param value
     * @throws BusinessException
     */
    public CustomFieldInstance setCFValue(ICustomFieldEntity entity, String code, Object value, User currentUser) throws BusinessException {

        log.debug("Setting CF value. Code: {}, entity {} value {}", code, entity, value);

        // Can not set the value if field is versionable without a date
        CustomFieldTemplate cft = customFieldsCacheContainerProvider.getCustomFieldTemplate(code, entity);
        if (cft == null) {
            throw new BusinessException("Custom field template with code " + code + " not found found for entity " + entity);
        }

        if (cft.isVersionable()) {
            throw new RuntimeException("Can not determine a period for Custom Field " + entity.getClass().getSimpleName() + "/" + code
                    + " value if no date or date range is provided");
        }

        List<CustomFieldInstance> cfis = getCustomFieldInstances(entity, code);
        CustomFieldInstance cfi = null;
        if (cfis.isEmpty()) {
            if (value == null) {
                return null;
            }
            cfi = CustomFieldInstance.fromTemplate(cft, entity);
            cfi.setValue(value);
            create(cfi, entity, currentUser, currentUser.getProvider());

        } else {
            cfi = cfis.get(0);
            cfi.setValue(value);
            cfi = update(cfi, entity, currentUser);
        }
        customFieldsCacheContainerProvider.addUpdateCustomFieldInCache(entity, cfi);
        return cfi;
    }

    public CustomFieldInstance setCFValue(ICustomFieldEntity entity, String code, Object value, Date valueDate, User currentUser) throws BusinessException {

        log.debug("Setting CF value. Code: {}, entity {} value {} valueDate {}", code, entity, value, valueDate);

        // If field is not versionable - set the value without the date
        CustomFieldTemplate cft = customFieldsCacheContainerProvider.getCustomFieldTemplate(code, entity);
        if (cft == null) {
            throw new BusinessException("Custom field template with code " + code + " not found found for entity " + entity);
        }

        if (!cft.isVersionable()) {
            setCFValue(entity, code, value, currentUser);

            // Calendar is needed to be able to set a value with a single date
        } else if (cft.getCalendar() == null) {
            log.error("Can not determine a period for Custom Field {}/{} value if no calendar is provided", entity.getClass().getSimpleName(), code);
            throw new RuntimeException("Can not determine a period for Custom Field " + entity.getClass().getSimpleName() + "/" + code + " value if no calendar is provided");
        }

        // Should not match more then one record as periods are calendar based
        List<CustomFieldInstance> cfis = getCustomFieldInstances(entity, code, valueDate);
        CustomFieldInstance cfi = null;
        if (cfis.isEmpty()) {
            // Nothing found and nothing to save
            if (value == null) {
                return null;
            }
            cfi = CustomFieldInstance.fromTemplate(cft, entity, valueDate);
            cfi.setValue(value);
            create(cfi, entity, currentUser, currentUser.getProvider());

        } else {
            cfi = cfis.get(0);
            cfi.setValue(value);
            cfi = update(cfi, entity, currentUser);
        }
        customFieldsCacheContainerProvider.addUpdateCustomFieldInCache(entity, cfi);
        return cfi;
    }

    public CustomFieldInstance setCFValue(ICustomFieldEntity entity, String code, Object value, Date valueDateFrom, Date valueDateTo, Integer valuePriority, User currentUser)
            throws BusinessException {

        log.debug("Setting CF value. Code: {}, entity {} value {} valueDateFrom {} valueDateTo {}", code, entity, value, valueDateFrom, valueDateTo);

        // If field is not versionable - set the value without the date
        CustomFieldTemplate cft = customFieldsCacheContainerProvider.getCustomFieldTemplate(code, entity);
        if (cft == null) {
            throw new BusinessException("Custom field template with code " + code + " not found found for entity " + entity);
        }

        if (!cft.isVersionable()) {
            setCFValue(entity, code, value, currentUser);

            // If calendar is provided - use calendar by the valueDateFrom date
        } else if (cft.getCalendar() != null) {
            log.warn(
                "Calendar is provided in Custom Field template {}/{} while trying to assign value period start and end dates with two values. Only start date will be considered",
                entity.getClass().getSimpleName(), code);
            setCFValue(entity, code, value, valueDateFrom, currentUser);
        }

        // Should not match more then one record
        List<CustomFieldInstance> cfis = getCustomFieldInstances(entity, code, valueDateFrom, valueDateTo);
        CustomFieldInstance cfi = null;
        if (cfis.isEmpty()) {
            if (value == null) {
                return null;
            }
            cfi = CustomFieldInstance.fromTemplate(cft, entity, valueDateFrom, valueDateTo, valuePriority);
            cfi.setValue(value);
            create(cfi, entity, currentUser, currentUser.getProvider());

        } else {
            cfi = cfis.get(0);
            cfi.setValue(value);
            cfi = update(cfi, entity, currentUser);
        }
        customFieldsCacheContainerProvider.addUpdateCustomFieldInCache(entity, cfi);

        return cfi;
    }

    /**
     * Remove Custom field instance
     * 
     * @param code Custom field code to remove
     */
    public void removeCFValue(ICustomFieldEntity entity, String code) {
        List<CustomFieldInstance> cfis = getCustomFieldInstances(entity, code);
        for (CustomFieldInstance cfi : cfis) {
            super.remove(cfi.getId());
        }

        customFieldsCacheContainerProvider.removeCustomFieldFromCache(entity, code);
    }

    /**
     * Remove all custom field values for a given entity
     * 
     * @param entity
     */
    public void removeCFValues(ICustomFieldEntity entity) {

        Map<String, List<CustomFieldInstance>> cfisByCode = getCustomFieldInstances(entity);
        for (Entry<String, List<CustomFieldInstance>> cfisInfo : cfisByCode.entrySet()) {
            for (CustomFieldInstance cfi : cfisInfo.getValue()) {
                super.remove(cfi.getId());
            }

            customFieldsCacheContainerProvider.removeCustomFieldFromCache(entity, cfisInfo.getKey());
        }
    }

    /**
     * Get All custom field instances for a given entity.
     * 
     * @param entity Entity
     * @return A map of Custom field instances with CF code as a key
     */
    public Map<String, List<CustomFieldInstance>> getCustomFieldInstances(ICustomFieldEntity entity) {
        if (((IEntity) entity).isTransient()) {
            return new HashMap<String, List<CustomFieldInstance>>();
        }

        TypedQuery<CustomFieldInstance> query = getEntityManager().createNamedQuery("CustomFieldInstance.getCfiByEntity", CustomFieldInstance.class);
        query.setParameter("appliesToEntity", entity.getUuid());
        query.setParameter("provider", getProvider(entity));

        List<CustomFieldInstance> cfis = query.getResultList();

        // // Make sure that embedded CF value property is not null
        // if (cfi != null && cfi.getCfValue() == null) {
        // cfi.setCfValue(new CustomFieldValue());
        // }

        Map<String, List<CustomFieldInstance>> cfisAsMap = new HashMap<String, List<CustomFieldInstance>>();

        for (CustomFieldInstance cfi : cfis) {
            if (!cfisAsMap.containsKey(cfi.getCode())) {
                cfisAsMap.put(cfi.getCode(), new ArrayList<CustomFieldInstance>());
            }
            cfisAsMap.get(cfi.getCode()).add(cfi);
        }

        return cfisAsMap;
    }

    /**
     * Get custom field instances for a given entity. Should be only a single record when custom field is not versioned
     * 
     * @param entity Entity
     * @param code Custom field code
     * @return Custom field instance
     */
    public List<CustomFieldInstance> getCustomFieldInstances(ICustomFieldEntity entity, String code) {

        TypedQuery<CustomFieldInstance> query = getEntityManager().createNamedQuery("CustomFieldInstance.getCfiByCode", CustomFieldInstance.class);
        query.setParameter("appliesToEntity", entity.getUuid());
        query.setParameter("code", code);
        query.setParameter("provider", getProvider(entity));

        List<CustomFieldInstance> cfis = query.getResultList();

        // // Make sure that embedded CF value property is not null
        // if (cfi != null && cfi.getCfValue() == null) {
        // cfi.setCfValue(new CustomFieldValue());
        // }

        return cfis;
    }

    /**
     * Get custom field instances for a given entity and a given date.
     * 
     * @param entity Entity
     * @param code Custom field code
     * @return Custom field instance
     */
    private List<CustomFieldInstance> getCustomFieldInstances(ICustomFieldEntity entity, String code, Date date) {

        TypedQuery<CustomFieldInstance> query = getEntityManager().createNamedQuery("CustomFieldInstance.getCfiByCodeAndDate", CustomFieldInstance.class);
        query.setParameter("appliesToEntity", entity.getUuid());
        query.setParameter("code", code);
        query.setParameter("provider", getProvider(entity));
        query.setParameter("date", date);

        List<CustomFieldInstance> cfis = query.getResultList();

        // // Make sure that embedded CF value property is not null
        // if (cfi != null && cfi.getCfValue() == null) {
        // cfi.setCfValue(new CustomFieldValue());
        // }

        return cfis;
    }

    /**
     * Get custom field instances for a given entity and a given date.
     * 
     * @param entity Entity
     * @param code Custom field code
     * @param valueDateFrom Value period data range - from
     * @param valueDateTo Value period data range - to
     * @return
     */
    private List<CustomFieldInstance> getCustomFieldInstances(ICustomFieldEntity entity, String code, Date valueDateFrom, Date valueDateTo) {

        TypedQuery<CustomFieldInstance> query = getEntityManager().createNamedQuery("CustomFieldInstance.getCfiByCodeAndDateRange", CustomFieldInstance.class);
        query.setParameter("appliesToEntity", entity.getUuid());
        query.setParameter("code", code);
        query.setParameter("provider", getProvider(entity));
        query.setParameter("dateFrom", valueDateFrom);
        query.setParameter("dateTo", valueDateTo);

        List<CustomFieldInstance> cfis = query.getResultList();

        // // Make sure that embedded CF value property is not null
        // if (cfi != null && cfi.getCfValue() == null) {
        // cfi.setCfValue(new CustomFieldValue());
        // }

        return cfis;
    }

    /**
     * Get provider of and entity. Handles cases when entity itself is a provider
     * 
     * @param entity Entity
     * @return Provider
     */
    private Provider getProvider(ICustomFieldEntity entity) {

        if (entity instanceof Provider) {
            if (((Provider) entity).isTransient()) {
                return null;
            }
            return (Provider) entity;

        } else {
            return ((IProvider) entity).getProvider();
        }
    }

    public Object getInheritedOnlyCFValue(ICustomFieldEntity entity, String code, User currentUser) {
        if (entity.getParentCFEntity() != null) {
            ICustomFieldEntity parentCFEntity = (ICustomFieldEntity) refreshOrRetrieveAny((IEntity) entity.getParentCFEntity());
            return getInheritedCFValue(parentCFEntity, code, currentUser);
        }
        return null;
    }

    public Object getInheritedOnlyCFValue(ICustomFieldEntity entity, String code, Date date, User currentUser) {

        if (entity.getParentCFEntity() != null) {
            ICustomFieldEntity parentCFEntity = (ICustomFieldEntity) refreshOrRetrieveAny((IEntity) entity.getParentCFEntity());
            return getInheritedCFValue(parentCFEntity, code, date, currentUser);
        }
        return null;
    }

    public Object getInheritedCFValue(ICustomFieldEntity entity, String code, User currentUser) {
        Object value = getCFValue(entity, code, currentUser);
        if (value == null && entity.getParentCFEntity() != null) {
            ICustomFieldEntity parentCFEntity = (ICustomFieldEntity) refreshOrRetrieveAny((IEntity) entity.getParentCFEntity());
            return getInheritedCFValue(parentCFEntity, code, currentUser);
        }
        return value;
    }

    public Object getInheritedCFValue(ICustomFieldEntity entity, String code, Date date, User currentUser) {

        Object value = getCFValue(entity, code, date, currentUser);
        if (value == null && entity.getParentCFEntity() != null) {
            ICustomFieldEntity parentCFEntity = (ICustomFieldEntity) refreshOrRetrieveAny((IEntity) entity.getParentCFEntity());
            return getInheritedCFValue(parentCFEntity, code, date, currentUser);
        }
        return value;
    }

    /**
     * Duplicate custom field values from one entity to another
     * 
     * @param sourceAppliesToEntity Source AppliesToEntity (UUID) value
     * @param entity New entity to copy custom field values to
     * @param currentUser User
     * @throws BusinessException
     */
    public void duplicateCfValues(String sourceAppliesToEntity, ICustomFieldEntity entity, User currentUser) throws BusinessException {
        TypedQuery<CustomFieldInstance> query = getEntityManager().createNamedQuery("CustomFieldInstance.getCfiByEntity", CustomFieldInstance.class);
        query.setParameter("appliesToEntity", sourceAppliesToEntity);
        query.setParameter("provider", getProvider(entity));

        List<CustomFieldInstance> cfis = query.getResultList();

        for (CustomFieldInstance cfi : cfis) {
            cfi.setId(null);
            cfi.setVersion(0);
            cfi.setAppliesToEntity(entity.getUuid());
            cfi.setAuditable(null);
            create(cfi, entity, currentUser, currentUser.getProvider());
        }
    }

    /**
     * A trigger when a future custom field end period event expired
     * 
     * @param timer Timer information
     */
    @Timeout
    private void triggerEndPeriodEventExpired(Timer timer) {
        log.debug("triggerEndPeriodEventExpired={}", timer);
        try {
            CustomFieldInstance cfi = (CustomFieldInstance) timer.getInfo();
            CFEndPeriodEvent event = new CFEndPeriodEvent();
            event.setCustomFieldInstance(cfi);
            cFEndPeriodEvent.fire(event);
        } catch (Exception e) {
            log.error("Failed executing end period event timer", e);
        }
    }

    /**
     * Initiate custom field end period event - either right away, or delay it for the future
     * 
     * @param cfi Custom field instance
     */
    private void triggerEndPeriodEvent(CustomFieldInstance cfi) {

        if (cfi.getPeriodEndDate() != null && cfi.getPeriodEndDate().before(new Date())) {
            CFEndPeriodEvent event = new CFEndPeriodEvent();
            event.setCustomFieldInstance(cfi);
            cFEndPeriodEvent.fire(event);

        } else if (cfi.getPeriodEndDate() != null) {

            TimerConfig timerConfig = new TimerConfig();
            timerConfig.setInfo(cfi);

            // used for testing
            // expiration = new Date();
            // expiration = DateUtils.addMinutes(expiration, 1);

            log.debug("Creating timer for triggerEndPeriodEvent for Custom field value {} with expiration={}", cfi, cfi.getPeriodEndDate());

            timerService.createSingleActionTimer(cfi.getPeriodEndDate(), timerConfig);
        }
    }

    private IEntity refreshOrRetrieveAny(IEntity entity) {

        if (getEntityManager().contains(entity)) {
            getEntityManager().refresh(entity);
            return entity;

        } else {
            entity = getEntityManager().find(PersistenceUtils.getClassForHibernateObject(entity), entity.getId());
            if (entity != null && isConversationScoped() && getCurrentProvider() != null) {
                if (entity instanceof BaseEntity) {
                    boolean notSameProvider = !((BaseEntity) entity).doesProviderMatch(getCurrentProvider());
                    if (notSameProvider) {
                        log.debug("CheckProvider in refreshOrRetrieveAny getCurrentProvider() id={}, entityProvider id={}", new Object[] { getCurrentProvider().getId(),
                                ((BaseEntity) entity).getProvider().getId() });
                        throw new ProviderNotAllowedException();
                    }
                }
            }
            return entity;
        }
    }

    /**
     * Match for a given entity's custom field (non-versionable values) as close as possible map's key to the key provided and return a map value. Match is performed by matching a
     * full string and then reducing one by one symbol until a match is found.
     * 
     * TODO can be an issue with lower/upper case mismatch
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param keyToMatch Key to match
     * @return Map value that closely matches map key
     */
    public Object getCFValueByClosestMatch(ICustomFieldEntity entity, String code, String keyToMatch) {

        Object value = getCFValue(entity, code, null);
        Object valueMatched = CustomFieldInstanceService.matchClosestValue(value, keyToMatch);

        log.trace("Found closest match value {} for keyToMatch={}", valueMatched, keyToMatch);
        return valueMatched;

    }

    /**
     * Match for a given date (versionable values) for a given entity's custom field as close as possible map's key to the key provided and return a map value. Match is performed
     * by matching a full string and then reducing one by one symbol until a match is found.
     * 
     * TODO can be an issue with lower/upper case mismatch
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param date Date
     * @param keyToMatch Key to match
     * @return Map value that closely matches map key
     */
    public Object getCFValueByClosestMatch(ICustomFieldEntity entity, String code, Date date, String keyToMatch) {
        Object value = getCFValue(entity, code, date, null);

        Object valueMatched = CustomFieldInstanceService.matchClosestValue(value, keyToMatch);
        log.trace("Found closest match value {} for period {} and keyToMatch={}", valueMatched, date, keyToMatch);
        return valueMatched;

    }

    /**
     * Match for a given entity's custom field (non-versionable values) map's key as the matrix value and return a map value.
     * 
     * Map key is assumed to be the following format:
     * <ul>
     * <li>MATRIX_STRING: <matrix first key>|<matrix second key></li>
     * <li>MATRIX_RON: <range of numbers for the first key>|<range of numbers for the second key></li>
     * </ul>
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param keyOne First key to match
     * @param keyTwo Second key to match
     * @return Map value that matches the matrix format map key
     */
    @SuppressWarnings("unchecked")
    public Object getCFValueByMatrix(ICustomFieldEntity entity, String code, Object keyOne, Object keyTwo) {

        CustomFieldTemplate cft = customFieldsCacheContainerProvider.getCustomFieldTemplate(code, entity);
        if (cft == null || cft.getMapKeyType() == null) {
            log.trace("No CFT found or map key type is unknown  {}/{}", entity, code);
            return null;
        }

        if (cft.getStorageType() != CustomFieldStorageTypeEnum.MATRIX) {
            log.trace("getCFValueByMatrix does not apply to storage type {}", cft.getStorageType());
            return null;
        }

        Map<String, Object> value = (Map<String, Object>) getCFValue(entity, code, null);
        Object valueMatched = CustomFieldInstanceService.matchMatrixValue(cft, value, keyOne, keyTwo);

        log.trace("Found matrix value match {} for keyToMatch={}|{}", valueMatched, keyOne, keyTwo);
        return valueMatched;

    }

    /**
     * Match for a given entity's custom field (versionable values) map's key as the matrix value and return a map value.
     * 
     * Map key is assumed to be the following format:
     * <ul>
     * <li>MATRIX_STRING: <matrix first key>|<matrix second key></li>
     * <li>MATRIX_RON: <range of numbers for the first key>|<range of numbers for the second key></li>
     * </ul>
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param date Date to match
     * @param keyOne First key to match
     * @param keyTwo Second key to match
     * @return Map value that matches the matrix format map key
     */
    public Object getCFValueByMatrix(ICustomFieldEntity entity, String code, Date date, Object keyOne, Object keyTwo) {

        CustomFieldTemplate cft = customFieldsCacheContainerProvider.getCustomFieldTemplate(code, entity);
        if (cft == null || cft.getMapKeyType() == null) {
            log.trace("No CFT found or map key type is unknown  {}/{}", entity, code);
            return null;
        }

        if (cft.getStorageType() != CustomFieldStorageTypeEnum.MATRIX) {
            log.trace("getCFValueByMatrix does not apply to storage type {}", cft.getStorageType());
            return null;
        }

        Object value = getCFValue(entity, code, date, null);
        Object valueMatched = CustomFieldInstanceService.matchMatrixValue(cft, value, keyOne, keyTwo);

        log.trace("Found matrix value match {} for period {} and keyToMatch={}|{}", valueMatched, date, keyOne, keyTwo);
        return valueMatched;

    }

    /**
     * Match for a given entity's custom field (non-versionable values) map's key as a range of numbers value and return a map value.
     * 
     * Number ranges is assumed to be the following format: <number from>&gt;<number to>
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param numberToMatch Number (long, integer, double, bigdecimal) value to match
     * @return Map value that matches the range of numbers in a map key
     */
    @SuppressWarnings("unchecked")
    public Object getCFValueByRangeOfNumbers(ICustomFieldEntity entity, String code, Object numberToMatch) {

        CustomFieldTemplate cft = customFieldsCacheContainerProvider.getCustomFieldTemplate(code, entity);
        if (cft == null || cft.getMapKeyType() == null) {
            log.trace("No CFT found or map key type is unknown  {}/{}", entity, code);
            return null;
        }

        if (!(cft.getStorageType() == CustomFieldStorageTypeEnum.MAP && cft.getMapKeyType() == CustomFieldMapKeyEnum.RON)) {
            log.trace("getCFValueByRangeOfNumbers does not apply to storage type {} and mapKeyType {}", cft.getStorageType(), cft.getMapKeyType());
            return null;
        }

        Map<String, Object> value = (Map<String, Object>) getCFValue(entity, code, null);
        Object valueMatched = CustomFieldInstanceService.matchRangeOfNumbersValue(value, numberToMatch);

        log.trace("Found matrix value match {} for numberToMatch={}", valueMatched, numberToMatch);
        return valueMatched;

    }

    /**
     * Match for a given entity's custom field (versionable values) map's key as a range of numbers value and return a map value.
     * 
     * Number ranges is assumed to be the following format: <number from>&gt;<number to>
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param date Date to match
     * @param numberToMatch Number (long, integer, double, bigdecimal) value to match
     * @return Map value that matches the range of numbers in a map key
     */
    public Object getCFValueByRangeOfNumbers(ICustomFieldEntity entity, String code, Date date, Object numberToMatch) {

        CustomFieldTemplate cft = customFieldsCacheContainerProvider.getCustomFieldTemplate(code, entity);
        if (cft == null || cft.getMapKeyType() == null) {
            log.trace("No CFT found or map key type is unknown  {}/{}", entity, code);
            return null;
        }

        if (!(cft.getStorageType() == CustomFieldStorageTypeEnum.MAP && cft.getMapKeyType() == CustomFieldMapKeyEnum.RON)) {
            log.trace("getCFValueByRangeOfNumbers does not apply to storage type {} and mapKeyType {}", cft.getStorageType(), cft.getMapKeyType());
            return null;
        }

        Object value = getCFValue(entity, code, date, null);
        Object valueMatched = CustomFieldInstanceService.matchRangeOfNumbersValue(value, numberToMatch);

        log.trace("Found matrix value match {} for period {} and numberToMatch={}", valueMatched, date, numberToMatch);
        return valueMatched;

    }

    /**
     * Match as close as possible map's key to the key provided and return a map value. Match is performed by matching a full string and then reducing one by one symbol untill a
     * match is found.
     * 
     * TODO can be an issue with lower/upper case mismatch
     * 
     * @param value Value to inspect
     * @param keyToMatch Key to match
     * @return Map value that closely matches map key
     */
    @SuppressWarnings("unchecked")
    public static Object matchClosestValue(Object value, String keyToMatch) {
        if (value == null || !(value instanceof Map) || StringUtils.isEmpty(keyToMatch)) {
            return null;
        }

        Object valueFound = null;
        Map<String, Object> mapValue = (Map<String, Object>) value;
        for (int i = keyToMatch.length(); i > 0; i--) {
            valueFound = mapValue.get(keyToMatch.substring(0, i));
            if (valueFound != null) {
                return valueFound;
            }
        }

        return null;
    }

    /**
     * Match for a given value map's key as the matrix value and return a map value.
     * 
     * Map key is assumed to be the following format:
     * <ul>
     * <li>MATRIX_STRING: <matrix first key>|<matrix second key></li>
     * <li>MATRIX_RON: <range of numbers for the first key>|<range of numbers for the second key></li>
     * </ul>
     * 
     * @param cft Custom field template
     * @param value Value to inspect
     * @param keyOne First key to match
     * @param keyTwo Second key to match
     * @return A value matched
     */
    @SuppressWarnings("unchecked")
    public static Object matchMatrixValue(CustomFieldTemplate cft, Object value, Object keyOne, Object keyTwo) {
        if (value == null || !(value instanceof Map) || keyOne == null || keyTwo == null || StringUtils.isEmpty(keyOne.toString()) || StringUtils.isEmpty(keyTwo.toString())) {
            return null;
        }

        Object valueMatched = null;
        if (cft.getMapKeyType() == CustomFieldMapKeyEnum.STRING) {
            String mapKey = keyOne + MATRIX_VALUE_SEPARATOR + keyTwo;
            valueMatched = ((Map<String, Object>) value).get(mapKey);

        } else if (cft.getMapKeyType() == CustomFieldMapKeyEnum.RON) {

            for (Entry<String, Object> valueInfo : ((Map<String, Object>) value).entrySet()) {
                String[] ranges = valueInfo.getKey().split("\\" + MATRIX_VALUE_SEPARATOR);
                if (isNumberRangeMatch(ranges[0], keyOne) && isNumberRangeMatch(ranges[1], keyTwo)) {
                    valueMatched = valueInfo.getValue();
                    break;
                }
            }
        }

        return valueMatched;
    }

    /**
     * Match map's key as a range of numbers value and return a matched value.
     * 
     * Number ranges is assumed to be the following format: <number from>&lt;<number to>
     * 
     * @param value Value to inspect
     * @param numberToMatch Number to match
     * @return Map value that closely matches map key
     */
    @SuppressWarnings("unchecked")
    public static Object matchRangeOfNumbersValue(Object value, Object numberToMatch) {
        if (value == null || !(value instanceof Map) || numberToMatch == null
                || !(numberToMatch instanceof Long || numberToMatch instanceof Integer || numberToMatch instanceof Double || numberToMatch instanceof BigDecimal)) {
            return null;
        }

        for (Entry<String, Object> valueInfo : ((Map<String, Object>) value).entrySet()) {
            if (isNumberRangeMatch(valueInfo.getKey(), numberToMatch)) {
                return valueInfo.getValue();
            }
        }

        return null;
    }

    /**
     * Determine if a number value is inside the number range expressed as <number from>&lt;<number to>
     * 
     * @param numberRange Number range value
     * @param numberToMatchObj A double number o
     * @return True if number have matched
     */
    private static boolean isNumberRangeMatch(String numberRange, Object numberToMatchObj) {
        String[] rangeInfo = numberRange.split(RON_VALUE_SEPARATOR);
        Double fromNumber = null;
        try {
            fromNumber = Double.parseDouble(rangeInfo[0]);
        } catch (NumberFormatException e) { // Ignore the error as value might be empty
        }
        Double toNumber = null;
        if (rangeInfo.length == 2) {
            try {
                toNumber = Double.parseDouble(rangeInfo[1]);
            } catch (NumberFormatException e) { // Ignore the error as value might be empty
            }
        }

        // Convert matching number to Double for further comparison
        Double numberToMatchDbl = null;
        if (numberToMatchObj instanceof Double) {
            numberToMatchDbl = (Double) numberToMatchObj;

        } else if (numberToMatchObj instanceof Integer) {
            numberToMatchDbl = ((Integer) numberToMatchObj).doubleValue();

        } else if (numberToMatchObj instanceof Long) {
            numberToMatchDbl = ((Long) numberToMatchObj).doubleValue();

        } else if (numberToMatchObj instanceof BigInteger) {
            numberToMatchDbl = ((BigInteger) numberToMatchObj).doubleValue();
        } else {
            Logger log = LoggerFactory.getLogger(CustomFieldInstanceService.class);
            log.error("Failed to match CF value for a range of numbers. Value passed is not a number {}", numberToMatchDbl);
            return false;
        }

        if (fromNumber != null && toNumber != null) {
            if (fromNumber.compareTo(numberToMatchDbl) <= 0 && toNumber.compareTo(numberToMatchDbl) > 0) {
                return true;
            }
        } else if (fromNumber != null) {
            if (fromNumber.compareTo(numberToMatchDbl) <= 0) {
                return true;
            }
        } else if (toNumber != null) {
            if (toNumber.compareTo(numberToMatchDbl) > 0) {
                return true;
            }
        }
        return false;
    }
}