# bee-cache

## 目標

* 多服務架構下，依舊使用本地緩存
* 減少第三方緩存服務的壓力
* 避免網路 __I/O__ 和資料結構轉換的消耗，提高效能
* 相容 __Spring Cacheable__


## 設計

由於緩存通常應用在不頻繁異動的資料上，所以不應該每次都向第三方緩存服務取得資料，而是透過 __MQ Service__ 廣播和訂閱訊息功能，發布註銷的事件，將對應的資料清除，大幅提高系統效能，降低龐大架構的維護成本

![](images/bee_cache.png)
![](images/bee_cache_query.png)
![](images/bee_cache_modify.png)


* 由於緩存通常應用在不頻繁異動的資料上
* 透過 __Message Queue Server__ 發布和訂閱訊息功能，通知所有服務註銷本地的緩存

## 使用

### Message Queue Service

如果是單服務，則不用實作

```java
@Log4j2
@Service
public class BeeCacheMQServiceImpl extends AbstractBeeCacheMQService implements  InitializingBean {
@Autowired
private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MyRedisMessageListenerContainer myRedisMessageListenerContainer;

    @Autowired
    private DefaultClientResources defaultClientResources;

    @Override
    public void afterPropertiesSet() throws Exception {
        defaultClientResources.eventBus().get().subscribe((event) -> {
            if (event instanceof ConnectedEvent) {
                publishConnectedEvent();
            }
        });
    }

    @Override
    public void send(String topic, String body) {
        stringRedisTemplate.convertAndSend(topic, body);
    }

    @Override
    public void addListener(String topic, Consumer<String> listener) {
        myRedisMessageListenerContainer.addMessageListener((message, pattern) -> {
            log.info("pattern:{},message:{}", new String(pattern), message);
            listener.accept(new String(message.getBody()));
        }, new PatternTopic(topic));
    }
}
```

### 緩存管理服務類型

* __BeeCacheManager__ 
    
    基本的緩存管理服務

    ```java
    // 單一服務架構
    new BeeCacheManager();

    // 多服務架構，配置 Message Queue Service 和 Topic
    new BeeCacheManager(topic, beeCacheMQService);
    ```
  

* __ExpireBeeCacheManager__

    固定失效時間的緩存管理服務，緩存從建立後，固定 __60__ 秒後就會失效

    ```java
    new ExpireBeeCacheManager(topic, beeCacheMQService, "PT60S");
    ```

* __BusyBeeCacheManager__

    讀取更新失效時間的緩存管理服務，緩存從建立後，如果 __60__ 秒內，沒有被讀取，則會失效

    ```java
    new BusyBeeCacheManager(topic, beeCacheMQService, "PT60S");
    ```


### 使用方式

參考 __Spring Cacheable__

* __@Cacheable__

  __一般__
  
  ```java
  @Cacheable(
      cacheNames = "User"
      , key = "#id"
      , condition = "#id != null"
      , unless = "#result==null"
  )
  public User find(Integer id) {
      // TODO
  }
  ```

  __時效性__

  ```java
  @Bean("ExpirePT60SName")
  public CacheManager ExpirePT1D(
          @Autowired(required = false) BeeCacheMQService beeCacheMQService
          , @Value("${cache.notify.topic:default}") String topic
  ) {
      return new ExpireBeeCacheManager(topic, beeCacheMQService, "PT60S");
  }
  
  @Cacheable(
      cacheNames = "User"
      , cacheManager = "ExpirePT60SName"
      , key = "#id"
      , condition = "#id != null"
      , unless = "#result==null"
  )
  public User find(Integer id) {
      // TODO
  }
  ```

* __@CacheEvict__

  如果有配置 __Message Queue Service__ 時，會將 __Evict Event__ 廣播給其他使用相同配置的服務

    __一般__
  
    ```java
    @CacheEvict(
        cacheNames = "User"
        , key = "#user.id"
        , condition = "#user.id != null"
    )
    public void update(User user) {
        //TODO
    }
    ```
    __時效性__

    ```java
    @CacheEvict(
        cacheNames = "User"
        , cacheManager = "ExpirePT60SName"
        , key = "#user.id"
        , condition = "#user.id != null"
    )
    public void update(User user) {
    
    }
    ```
  
* 註冊 __Evict__ 和 __Clear__ 的事件處理
  
  收到 __Evict__ 和 __Clear Event__ ，透過 __handler__ 取得最新資料，直接更新緩存，避免同時請求造成多次查詢或者鎖的等待

  ```java
  BeeCacheManager.onEvict(
          UserCacheKey
          , (data) -> find(data.getId())
          , User.class
  );
  ```
