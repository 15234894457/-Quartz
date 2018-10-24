# -Quartz
QuartzConfiguration
quartz与Spring相关框架的整合方式有很多种，我们今天采用jobDetail使用Spring Ioc托管方式来完成整合，我们可以在定时任务实例中使用Spring注入注解完成业务逻辑处理，下面我先把全部的配置贴出来再逐步分析，配置类如下所示：

package com.hengyu.chapter39.configuration;

import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import javax.sql.DataSource;

/**
 * quartz定时任务配置
 * ========================
 * Created with IntelliJ IDEA.
 * User：恒宇少年
 * Date：2017/11/5
 * Time：14:07
 * 码云：http://git.oschina.net/jnyqy
 * ========================
 * @author  恒宇少年
 */
@Configuration
@EnableScheduling
public class QuartzConfiguration
{
    /**
     * 继承org.springframework.scheduling.quartz.SpringBeanJobFactory
     * 实现任务实例化方式
     */
    public static class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory implements
            ApplicationContextAware {

        private transient AutowireCapableBeanFactory beanFactory;

        @Override
        public void setApplicationContext(final ApplicationContext context) {
            beanFactory = context.getAutowireCapableBeanFactory();
        }

        /**
         * 将job实例交给spring ioc托管
         * 我们在job实例实现类内可以直接使用spring注入的调用被spring ioc管理的实例
         * @param bundle
         * @return
         * @throws Exception
         */
        @Override
        protected Object createJobInstance(final TriggerFiredBundle bundle) throws Exception {
            final Object job = super.createJobInstance(bundle);
            /**
             * 将job实例交付给spring ioc
             */
            beanFactory.autowireBean(job);
            return job;
        }
    }

    /**
     * 配置任务工厂实例
     * @param applicationContext spring上下文实例
     * @return
     */
    @Bean
    public JobFactory jobFactory(ApplicationContext applicationContext)
    {
        /**
         * 采用自定义任务工厂 整合spring实例来完成构建任务
         * see {@link AutowiringSpringBeanJobFactory}
         */
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    /**
     * 配置任务调度器
     * 使用项目数据源作为quartz数据源
     * @param jobFactory 自定义配置任务工厂
     * @param dataSource 数据源实例
     * @return
     * @throws Exception
     */
    @Bean(destroyMethod = "destroy",autowire = Autowire.NO)
    public SchedulerFactoryBean schedulerFactoryBean(JobFactory jobFactory, DataSource dataSource) throws Exception
    {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        //将spring管理job自定义工厂交由调度器维护
        schedulerFactoryBean.setJobFactory(jobFactory);
        //设置覆盖已存在的任务
        schedulerFactoryBean.setOverwriteExistingJobs(true);
        //项目启动完成后，等待2秒后开始执行调度器初始化
        schedulerFactoryBean.setStartupDelay(2);
        //设置调度器自动运行
        schedulerFactoryBean.setAutoStartup(true);
        //设置数据源，使用与项目统一数据源
        schedulerFactoryBean.setDataSource(dataSource);
        //设置上下文spring bean name
        schedulerFactoryBean.setApplicationContextSchedulerContextKey("applicationContext");
        //设置配置文件位置
        schedulerFactoryBean.setConfigLocation(new ClassPathResource("/quartz.properties"));
        return schedulerFactoryBean;
    }
}
AutowiringSpringBeanJobFactory
可以看到上面配置类中，AutowiringSpringBeanJobFactory我们继承了SpringBeanJobFactory类，并且通过实现ApplicationContextAware接口获取ApplicationContext设置方法，通过外部实例化时设置ApplicationContext实例对象，在createJobInstance方法内，我们采用AutowireCapableBeanFactory来托管SpringBeanJobFactory类中createJobInstance方法返回的定时任务实例，这样我们就可以在定时任务类内使用Spring Ioc相关的注解进行注入业务逻辑实例了。

JobFactory
任务工厂是在本章配置调度器时所需要的实例，我们通过jobFactory方法注入ApplicationContext实例，来创建一个AutowiringSpringBeanJobFactory对象，并且将对象实例托管到Spring Ioc容器内。

SchedulerFactoryBean
我们本章采用的是项目内部数据源的方式来设置调度器的jobSotre，官方quartz有两种持久化的配置方案。

第一种：采用quartz.properties配置文件配置独立的定时任务数据源，可以与使用项目的数据库完全独立。
第二种：采用与创建项目统一个数据源，定时任务持久化相关的表与业务逻辑在同一个数据库内。

可以根据实际的项目需求采取不同的方案，我们本章主要是通过第二种方案来进行讲解，在上面配置类中可以看到方法schedulerFactoryBean内自动注入了JobFactory实例，也就是我们自定义的AutowiringSpringBeanJobFactory任务工厂实例，另外一个参数就是DataSource，在我们引入spring-starter-data-jpa依赖后会根据application.yml文件内的数据源相关配置自动实例化DataSource实例，这里直接注入是没有问题的。

我们通过调用SchedulerFactoryBean对象的setConfigLocation方法来设置quartz定时任务框架的基本配置，配置文件所在位置：resources/quartz.properties => classpath:/quartz.properties下。

注意：quartz.properties配置文件一定要放在classpath下，放在别的位置有部分功能不会生效。

下面我们来看下quartz.properties文件内的配置，如下所示：

#调度器实例名称
org.quartz.scheduler.instanceName = quartzScheduler

#调度器实例编号自动生成
org.quartz.scheduler.instanceId = AUTO

#持久化方式配置
org.quartz.jobStore.class = org.quartz.impl.jdbcjobstore.JobStoreTX

#持久化方式配置数据驱动，MySQL数据库
org.quartz.jobStore.driverDelegateClass = org.quartz.impl.jdbcjobstore.StdJDBCDelegate

#quartz相关数据表前缀名
org.quartz.jobStore.tablePrefix = QRTZ_

#开启分布式部署
org.quartz.jobStore.isClustered = true
#配置是否使用
org.quartz.jobStore.useProperties = false

#分布式节点有效性检查时间间隔，单位：毫秒
org.quartz.jobStore.clusterCheckinInterval = 20000

#线程池实现类
org.quartz.threadPool.class = org.quartz.simpl.SimpleThreadPool

#执行最大并发线程数量
org.quartz.threadPool.threadCount = 10

#线程优先级
org.quartz.threadPool.threadPriority = 5

#配置为守护线程，设置后任务将不会执行
#org.quartz.threadPool.makeThreadsDaemons=true

#配置是否启动自动加载数据库内的定时任务，默认true
org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread = true
由于我们下一章需要做分布式多节点自动交付高可用，本章的配置文件加入了分布式相关的配置。
在上面配置中org.quartz.jobStore.class与org.quartz.jobStore.driverDelegateClass是定时任务持久化的关键配置，配置了数据库持久化定时任务以及采用MySQL数据库进行连接，当然这里我们也可以配置其他的数据库，如下所示：
PostgreSQL ： org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
Sybase : org.quartz.impl.jdbcjobstore.SybaseDelegate
MSSQL : org.quartz.impl.jdbcjobstore.MSSQLDelegate
HSQLDB : org.quartz.impl.jdbcjobstore.HSQLDBDelegate
Oracle : org.quartz.impl.jdbcjobstore.oracle.OracleDelegate

org.quartz.jobStore.tablePrefix属性配置了定时任务数据表的前缀，在quartz官方提供的创建表SQL脚本默认就是qrtz_，在对应的XxxDelegate驱动类内也是使用的默认值，所以这里我们如果修改表名前缀，配置可以去掉。

org.quartz.jobStore.isClustered属性配置了开启定时任务分布式功能，再开启分布式时对应属性org.quartz.scheduler.instanceId 改成Auto配置即可，实例唯一标识会自动生成，这个标识具体生成的内容，我们一会在运行的控制台就可以看到了，定时任务分布式准备好后会输出相关的分布式节点配置信息。

创建表SQL会在本章源码resources目录下，源码地址https://gitee.com/hengboy/spring-boot-chapter。

准备测试
我们先来创建一个简单的商品数据表，建表SQL如下所示：

DROP TABLE IF EXISTS `basic_good_info`;
CREATE TABLE `basic_good_info` (
  `BGI_ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '商品编号',
  `BGI_NAME` varchar(20) DEFAULT NULL COMMENT '商品名称',
  `BGI_PRICE` decimal(8,2) DEFAULT NULL COMMENT '单价',
  `BGI_UNIT` varchar(10) DEFAULT NULL COMMENT '单位',
  PRIMARY KEY (`BGI_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8 COMMENT='商品基本信息';
GoodEntity
我们先来针对表basic_good_info创建一个实体，并且添加JPA相关的配置，如下所示：

package com.hengyu.chapter39.good.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * ========================
 *
 * @author 恒宇少年
 * Created with IntelliJ IDEA.
 * Date：2017/11/5
 * Time：14:59
 * 码云：http://git.oschina.net/jnyqy
 * ========================
 */
@Entity
@Table(name = "basic_good_info")
@Data
public class GoodInfoEntity
{
    /**
     * 商品编号
     */
    @Id
    @GeneratedValue
    @Column(name = "bgi_id")
    private Long id;
    /**
     * 商品名称
     */
    @Column(name = "bgi_name")
    private String name;
    /**
     * 商品单位
     */
    @Column(name = "bgi_unit")
    private String unit;
    /**
     * 商品单价
     */
    @Column(name = "bgi_price")
    private BigDecimal price;
}
下面我们根据商品实体来创建JPA接口，如下所示：

/**
 * ========================
 * Created with IntelliJ IDEA.
 * Date：2017/11/5
 * Time：14:55
 * 码云：http://git.oschina.net/jnyqy
 * ========================
 * @author 恒宇少年
 */
public interface GoodInfoRepository
    extends JpaRepository<GoodInfoEntity,Long>
{
}
接下来我们再来添加一个商品添加的控制器方法，如下所示：

/**
 * ========================
 *
 * @author 恒宇少年
 * Created with IntelliJ IDEA.
 * Date：2017/11/5
 * Time：15:02
 * 码云：http://git.oschina.net/jnyqy
 * ========================
 */
@RestController
@RequestMapping(value = "/good")
public class GoodController
{
    /**
     * 商品业务逻辑实现
     */
    @Autowired
    private GoodInfoService goodInfoService;
    /**
     * 添加商品
     * @return
     */
    @RequestMapping(value = "/save")
    public Long save(GoodInfoEntity good) throws Exception
    {
        return goodInfoService.saveGood(good);
    }
}
在请求商品添加方法时，我们调用了GoodInfoService内的saveGood方法，传递一个商品的实例作为参数。我们接下来看看该类内相关代码，如下所示：

/**
 * 商品业务逻辑
 * ========================
 *
 * @author 恒宇少年
 * Created with IntelliJ IDEA.
 * Date：2017/11/5
 * Time：15:04
 * 码云：http://git.oschina.net/jnyqy
 * ========================
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class GoodInfoService
{
    /**
     * 注入任务调度器
     */
    @Autowired
    private Scheduler scheduler;
    /**
     * 商品数据接口
     */
    @Autowired
    private GoodInfoRepository goodInfoRepository;

    /**
     * 保存商品基本信息
     * @param good 商品实例
     * @return
     */
    public Long saveGood(GoodInfoEntity good) throws Exception
    {
        goodInfoRepository.save(good);
        return good.getId();
    }
我们只是作为保存商品的操作，下面我们来模拟一个需求，在商品添加完成后1分钟我们通知后续的逻辑进行下一步处理，同时开始商品库存定时检查的任务。

定义商品添加定时任务
我们先来创建一个任务实例，并且继承org.springframework.scheduling.quartz.QuartzJobBean抽象类，重写父抽象类内的executeInternal方法来实现任务的主体逻辑。如下所示：

/**
 * 商品添加定时任务实现类
 * ========================
 * Created with IntelliJ IDEA.
 * User：恒宇少年
 * Date：2017/11/5
 * Time：14:47
 * 码云：http://git.oschina.net/jnyqy
 * ========================
 * @author 恒宇少年
 */
public class GoodAddTimer
    extends QuartzJobBean
{
    /**
     * logback
     */
    static Logger logger = LoggerFactory.getLogger(GoodAddTimer.class);
    /**
     * 定时任务逻辑实现方法
     * 每当触发器触发时会执行该方法逻辑
     * @param jobExecutionContext 任务执行上下文
     * @throws JobExecutionException
     */
    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.info("商品添加完成后执行任务，任务时间：{}",new Date());
    }
在任务主体逻辑内，我们只是做了一个简单的输出任务执行的时间，下面我们再来创建库存定时检查任务。

定义商品库存检查任务
同样需要继承org.springframework.scheduling.quartz.QuartzJobBean抽象类实现抽象类内的executeInternal方法，如下所示：

/**
 * 商品库存检查定时任务
 * ========================
 *
 * @author 恒宇少年
 * Created with IntelliJ IDEA.
 * Date：2017/11/5
 * Time：15:47
 * 码云：http://git.oschina.net/jnyqy
 * ========================
 */
public class GoodStockCheckTimer
    extends QuartzJobBean
{
    /**
     * logback
     */
    static Logger logger = LoggerFactory.getLogger(GoodStockCheckTimer.class);

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.info("执行库存检查定时任务，执行时间：{}",new Date());
    }
}
都是简单的做了下日志的输出，下面我们需要重构GoodInfoService内的saveGood方法，对应的添加上面两个任务的创建。

设置商品添加任务到调度器
在GoodInfoService类内添加buildCreateGoodTimer方法用于实例化商品添加任务，如下所示：

/**
     * 构建创建商品定时任务
     */
    public void buildCreateGoodTimer() throws Exception
    {
        //设置开始时间为1分钟后
        long startAtTime = System.currentTimeMillis() + 1000 * 60;
        //任务名称
        String name = UUID.randomUUID().toString();
        //任务所属分组
        String group = GoodAddTimer.class.getName();
        //创建任务
        JobDetail jobDetail = JobBuilder.newJob(GoodAddTimer.class).withIdentity(name,group).build();
        //创建任务触发器
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity(name,group).startAt(new Date(startAtTime)).build();
        //将触发器与任务绑定到调度器内
        scheduler.scheduleJob(jobDetail, trigger);
    }
在上面方法中我们定义的GoodAddTimer实例只运行一次，在商品添加完成后延迟1分钟进行调用任务主体逻辑。

其中任务的名称以及任务的分组是为了区分任务做的限制，在同一个分组下如果加入同样名称的任务，则会提示任务已经存在，添加失败的提示。

我们通过JobDetail来构建一个任务实例，设置GoodAddTimer类作为任务运行目标对象，当任务被触发时就会执行GoodAddTimer内的executeInternal方法。

一个任务需要设置对应的触发器，触发器也分为很多种，该任务中我们并没有采用cron表达式来设置触发器，而是调用startAt方法设置任务开始执行时间。

最后将任务以及任务的触发器共同交付给任务调度器，这样就完成了一个任务的设置。

设置商品库存检查到任务调度器
在GoodInfoService类内添加buildGoodStockTimer方法用于实例化商品添加任务，如下所示：

/**
     * 构建商品库存定时任务
     * @throws Exception
     */
    public void buildGoodStockTimer() throws Exception
    {
        //任务名称
        String name = UUID.randomUUID().toString();
        //任务所属分组
        String group = GoodStockCheckTimer.class.getName();

        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0/30 * * * * ?");
        //创建任务
        JobDetail jobDetail = JobBuilder.newJob(GoodStockCheckTimer.class).withIdentity(name,group).build();
        //创建任务触发器
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity(name,group).withSchedule(scheduleBuilder).build();
        //将触发器与任务绑定到调度器内
        scheduler.scheduleJob(jobDetail, trigger);
    }
该任务的触发器我们采用了cron表达式来设置，每隔30秒执行一次任务主体逻辑。

任务触发器在创建时cron表达式可以搭配startAt方法来同时使用。

下面我们修改GoodInfoService内的saveGood方法，分别调用设置任务的两个方法，如下所示：

/**
     * 保存商品基本信息
     * @param good 商品实例
     * @return
     */
    public Long saveGood(GoodInfoEntity good) throws Exception
    {
        goodInfoRepository.save(good);
        //构建创建商品定时任务
        buildCreateGoodTimer();
        //构建商品库存定时任务
        buildGoodStockTimer();
        return good.getId();
    }
下面我们就来测试下任务是否可以顺序的被持久化到数据库，并且是否可以在重启服务后执行重启前添加的任务。

测试
下面我们来启动项目，启动成功后，我们来查看控制台输出的分布式节点的信息，如下所示：

2017-11-05 18:09:40.052  INFO 7708 --- [           main] c.hengyu.chapter39.Chapter39Application  : 【【【【【【定时任务分布式节点 - 1 已启动】】】】】】
2017-11-05 18:09:42.005  INFO 7708 --- [lerFactoryBean]] o.s.s.quartz.SchedulerFactoryBean        : Starting Quartz Scheduler now, after delay of 2 seconds
2017-11-05 18:09:42.027  INFO 7708 --- [lerFactoryBean]] o.s.s.quartz.LocalDataSourceJobStore     : ClusterManager: detected 1 failed or restarted instances.
2017-11-05 18:09:42.027  INFO 7708 --- [lerFactoryBean]] o.s.s.quartz.LocalDataSourceJobStore     : ClusterManager: Scanning for instance "yuqiyu1509876084785"'s failed in-progress jobs.
2017-11-05 18:09:42.031  INFO 7708 --- [lerFactoryBean]] o.s.s.quartz.LocalDataSourceJobStore     : ClusterManager: ......Freed 1 acquired trigger(s).
2017-11-05 18:09:42.033  INFO 7708 --- [lerFactoryBean]] org.quartz.core.QuartzScheduler          : Scheduler schedulerFactoryBean_$_yuqiyu1509876579404 started.
定时任务是在项目启动后2秒进行执行初始化，并且通过ClusterManager来完成了instance的创建，创建的节点唯一标识为yuqiyu1509876084785。

编写商品控制器请求方法测试用例，如下所示：

@RunWith(SpringRunner.class)
@SpringBootTest
public class Chapter39ApplicationTests {
    /**
     * 模拟mvc测试对象
     */
    private MockMvc mockMvc;

    /**
     * web项目上下文
     */
    @Autowired
    private WebApplicationContext webApplicationContext;

    /**
     * 所有测试方法执行之前执行该方法
     */
    @Before
    public void before() {
        //获取mockmvc对象实例
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /**
     * 测试添加商品
     * @throws Exception
     */
    @Test
    public void addGood() throws Exception
    {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/good/save")
                .param("name","西瓜")
                .param("unit","斤")
                .param("price","12.88")
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is(200))
                .andReturn();
        result.getResponse().setCharacterEncoding("UTF-8");
        System.out.println(result.getResponse().getContentAsString());
    }
测试用例相关文章请访问第三十五章：SpringBoot与单元测试的小秘密，我们来执行addGood测试方法，查看控制台输出，如下所示：

....省略部分输出
Hibernate: insert into basic_good_info (bgi_name, bgi_price, bgi_unit) values (?, ?, ?)
2017-11-05 18:06:35.699 TRACE 7560 --- [           main] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [VARCHAR] - [西瓜]
2017-11-05 18:06:35.701 TRACE 7560 --- [           main] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [NUMERIC] - [12.88]
2017-11-05 18:06:35.701 TRACE 7560 --- [           main] o.h.type.descriptor.sql.BasicBinder      : binding parameter [3] as [VARCHAR] - [斤]
....省略部分输出
8
....省略部分输出
可以看到我们的商品已被成功的写入到数据库并且输出的主键值，我们的任务是否也成功的被写入到数据库了呢？我们来查看qrtz_job_details表内任务列表，如下所示：

schedulerFactoryBean    7567c9d7-76f5-47f3-bc5d-b934f4c1063b    com.hengyu.chapter39.timers.GoodStockCheckTimer     com.hengyu.chapter39.timers.GoodStockCheckTimer 0   0   0   0   0xACED0005737200156F72672E71756172747A2E4A6F62446174614D61709FB083E8BFA9B0CB020000787200266F72672E71756172747A2E7574696C732E537472696E674B65794469727479466C61674D61708208E8C3FBC55D280200015A0013616C6C6F77735472616E7369656E74446174617872001D6F72672E71756172747A2E7574696C732E4469727479466C61674D617013E62EAD28760ACE0200025A000564697274794C00036D617074000F4C6A6176612F7574696C2F4D61703B787000737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F40000000000010770800000010000000007800
schedulerFactoryBean    e5e08ab0-9be3-43fb-93b8-b9490432a5d7    com.hengyu.chapter39.timers.GoodAddTimer        com.hengyu.chapter39.timers.GoodAddTimer    0   0   0   0   0xACED0005737200156F72672E71756172747A2E4A6F62446174614D61709FB083E8BFA9B0CB020000787200266F72672E71756172747A2E7574696C732E537472696E674B65794469727479466C61674D61708208E8C3FBC55D280200015A0013616C6C6F77735472616E7369656E74446174617872001D6F72672E71756172747A2E7574696C732E4469727479466C61674D617013E62EAD28760ACE0200025A000564697274794C00036D617074000F4C6A6176612F7574696C2F4D61703B787000737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F40000000000010770800000010000000007800

任务已经被成功的持久化到数据库内，等待1分钟后查看控制台输出内容如下所示：

2017-11-05 18:12:30.017  INFO 7708 --- [ryBean_Worker-1] c.h.c.timers.GoodStockCheckTimer         : 执行库存检查定时任务，执行时间：Sun Nov 05 18:12:30 CST 2017
2017-11-05 18:13:00.009  INFO 7708 --- [ryBean_Worker-2] c.h.c.timers.GoodStockCheckTimer         : 执行库存检查定时任务，执行时间：Sun Nov 05 18:13:00 CST 2017
2017-11-05 18:13:02.090  INFO 7708 --- [ryBean_Worker-3] c.hengyu.chapter39.timers.GoodAddTimer   : 商品添加完成后执行任务，任务时间：Sun Nov 05 18:13:02 CST 2017
根据输出的内容来判定完全吻合我们的配置参数，库存检查为30秒执行一次，而添加成功后的提醒则是1分钟后执行一次。执行完成后就会被直接销毁，我们再来查看数据库表qrtz_job_details，这时就可以看到还剩下1个任务。

重启服务任务是否自动执行？
下面我们把项目重启下，然后观察控制台的输出内容，如下所示：

2017-11-05 18:15:54.018  INFO 7536 --- [           main] c.hengyu.chapter39.Chapter39Application  : 【【【【【【定时任务分布式节点 - 1 已启动】】】】】】
2017-11-05 18:15:55.975  INFO 7536 --- [lerFactoryBean]] o.s.s.quartz.SchedulerFactoryBean        : Starting Quartz Scheduler now, after delay of 2 seconds
2017-11-05 18:15:56.000  INFO 7536 --- [lerFactoryBean]] org.quartz.core.QuartzScheduler          : Scheduler schedulerFactoryBean_$_yuqiyu1509876953202 started.
2017-11-05 18:16:15.999  INFO 7536 --- [_ClusterManager] o.s.s.quartz.LocalDataSourceJobStore     : ClusterManager: detected 1 failed or restarted instances.
2017-11-05 18:16:16.000  INFO 7536 --- [_ClusterManager] o.s.s.quartz.LocalDataSourceJobStore     : ClusterManager: Scanning for instance "yuqiyu1509876579404"'s failed in-progress jobs.
2017-11-05 18:16:16.005  INFO 7536 --- [_ClusterManager] o.s.s.quartz.LocalDataSourceJobStore     : ClusterManager: ......Freed 1 acquired trigger(s).
2017-11-05 18:16:16.041  INFO 7536 --- [ryBean_Worker-1] c.h.c.timers.GoodStockCheckTimer         : 执行库存检查定时任务，执行时间：Sun Nov 05 18:16:16 CST 2017
可以看到成功的自动执行了我们在重启之前配置的任务。

作者：恒宇少年
链接：https://www.jianshu.com/p/d52d62fb2ac6
來源：简书
简书著作权归作者所有，任何形式的转载都请联系作者获得授权并注明出处。
