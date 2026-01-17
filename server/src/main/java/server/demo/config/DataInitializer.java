package server.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import server.demo.entity.AutoMessage;
import server.demo.entity.Channel;
import server.demo.entity.Cleaner;
import server.demo.entity.CleaningConfig;
import server.demo.entity.CleaningSupply;
import server.demo.entity.NotificationSetting;
import server.demo.entity.QuickReply;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.Store;
import server.demo.entity.StorePolicy;
import server.demo.entity.User;
import server.demo.enums.ChannelType;
import server.demo.enums.ReservationStatus;
import server.demo.repository.AutoMessageRepository;
import server.demo.repository.ChannelRepository;
import server.demo.repository.CleanerRepository;
import server.demo.repository.CleaningConfigRepository;
import server.demo.repository.CleaningSupplyRepository;
import server.demo.repository.NotificationSettingRepository;
import server.demo.repository.QuickReplyRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StorePolicyRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// 数据初始化器,用于在应用启动时创建测试数据
// 使用 @Profile("init-data") 注解，只在指定 profile 时才运行
// 要启用数据初始化，在 application.properties 中添加: spring.profiles.active=init-data
@Component
@Profile("init-data")
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StorePolicyRepository storePolicyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuickReplyRepository quickReplyRepository;

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Autowired
    private AutoMessageRepository autoMessageRepository;

    @Autowired
    private CleaningConfigRepository cleaningConfigRepository;

    @Autowired
    private CleanerRepository cleanerRepository;

    @Autowired
    private CleaningSupplyRepository cleaningSupplyRepository;

    @Autowired
    private RoomPriceRepository roomPriceRepository;

    @Override
    public void run(String... args) throws Exception {
        // 每次启动都清空room_prices表中的available_rooms字段,让系统动态计算
        System.out.println("===== 清理房价表中的可用房间数 =====");
        List<server.demo.entity.RoomPrice> allPrices = roomPriceRepository.findAll();
        int clearedCount = 0;
        for (server.demo.entity.RoomPrice rp : allPrices) {
            if (rp.getAvailableRooms() != null) {
                rp.setAvailableRooms(null);
                clearedCount++;
            }
        }
        if (clearedCount > 0) {
            roomPriceRepository.saveAll(allPrices);
            System.out.println("已清理 " + clearedCount + " 条记录的available_rooms字段");
        } else {
            System.out.println("无需清理");
        }
        System.out.println("======================================\n");

        // 检查是否已有数据
        if (roomTypeRepository.count() > 0) {
            return;
        }

        // 初始化默认用户 (ID=1)
        System.out.println("===== 检查并创建默认用户 =====");
        User defaultUser = userRepository.findById(1L).orElse(null);
        if (defaultUser == null) {
            defaultUser = new User();
            defaultUser.setUsername("admin");
            defaultUser.setEmail("admin@hotel.com");
            defaultUser.setPassword(passwordEncoder.encode("admin123"));
            defaultUser.setName("系统管理员");
            defaultUser.setNickname("Admin");
            defaultUser.setIsActive(true);
            defaultUser = userRepository.save(defaultUser);
            System.out.println("默认用户创建成功: username=admin, email=admin@hotel.com, password=admin123");
        } else {
            System.out.println("默认用户已存在: " + defaultUser.getUsername());
        }
        System.out.println("======================================\n");

        // 初始化门店数据
        Store defaultStore = null;
        if (storeRepository.count() == 0) {
            Store store = new Store();
            store.setUserId(1L); // 设置默认用户ID为1
            store.setName("薫蔵商事株式会社");
            store.setPhone("+81-09094295658");
            store.setType("日式旅馆");
            store.setTimezone("Asia/Tokyo");
            store.setManager("林经理");
            store.setOwnerEmail("kunzo@example.com");
            store.setAddress("東京都渋谷区神宮前1-1-1");
            store.setCity("東京");
            store.setState("東京都");
            store.setCountry("Japan");
            store.setCurrency("JPY");
            store.setEmail("info@kunzo.jp");
            store.setWechat("kunzo_wechat");
            store.setWhatsapp("+81-09094295658");
            store.setLine("kunzo_line");
            store.setLanguage("日本語");
            store.setDescription("位于东京涩谷的精品日式旅馆,提供传统日式住宿体验");

            defaultStore = storeRepository.save(store);

            // 初始化门店政策
            StorePolicy policy = new StorePolicy();
            policy.setStore(defaultStore);
            policy.setCheckinTime("14:00 之后");
            policy.setCheckoutTime("11:00 之前");
            policy.setChildPolicy("允许儿童入住。不提供加床服务。");
            policy.setSmokingPolicy("禁止吸烟");
            policy.setPetPolicy("不允许携带宠物");
            policy.setAdditionalRules("请保持安静,尊重其他客人\n请爱护房间设施\n退房时请将房间钥匙交还前台");
            policy.setHotelTerms("1. 入住时需出示有效身份证件\n2. 押金政策:入住时需支付押金\n3. 取消政策:入住前24小时可免费取消\n4. 损坏赔偿:如有设施损坏需照价赔偿");

            storePolicyRepository.save(policy);

            System.out.println("门店数据初始化完成");
        } else {
            // 如果门店已存在，获取第一个门店
            defaultStore = storeRepository.findById(1L).orElse(null);
        }

        // 确保有默认门店
        if (defaultStore == null) {
            throw new RuntimeException("默认门店不存在，无法初始化数据");
        }

        // 初始化房型数据
        RoomType daBedRoom = new RoomType("大床房", "DBF", 10, "舒适大床房");
        daBedRoom.setUser(defaultUser);
        daBedRoom.setStoreId(defaultStore.getId());
        RoomType standardRoom = new RoomType("标准间", "BZJ", 15, "标准双人间");
        standardRoom.setUser(defaultUser);
        standardRoom.setStoreId(defaultStore.getId());
        RoomType suite = new RoomType("套房", "TF", 5, "豪华套房");
        suite.setUser(defaultUser);
        suite.setStoreId(defaultStore.getId());

        roomTypeRepository.save(daBedRoom);
        roomTypeRepository.save(standardRoom);
        roomTypeRepository.save(suite);

        // 初始化房间数据
        // 大床房
        for (int i = 1; i <= 10; i++) {
            Room room = new Room("a" + String.format("%02d", i), daBedRoom, 1);
            roomRepository.save(room);
        }

        // 标准间
        for (int i = 1; i <= 15; i++) {
            Room room = new Room("b" + String.format("%02d", i), standardRoom, 2);
            roomRepository.save(room);
        }

        // 套房
        for (int i = 1; i <= 5; i++) {
            Room room = new Room("c" + String.format("%02d", i), suite, 3);
            roomRepository.save(room);
        }

        // 初始化渠道数据
        Channel directChannel = new Channel("自来客", "DIRECT", ChannelType.DIRECT);
        directChannel.setUser(defaultUser);
        directChannel.setStoreId(defaultStore.getId());
        directChannel.setColor("#409EFF");
        directChannel.setEnabled(true);
        directChannel.setDescription("直接预订客户");

        Channel manualChannel = new Channel("手动录入", "MANUAL", ChannelType.DIRECT);
        manualChannel.setUser(defaultUser);
        manualChannel.setStoreId(defaultStore.getId());
        manualChannel.setColor("#409EFF");
        manualChannel.setEnabled(true);
        manualChannel.setDescription("手动录入订单");

        Channel airbnbChannel = new Channel("Airbnb", "AIRBNB", ChannelType.OTA);
        airbnbChannel.setUser(defaultUser);
        airbnbChannel.setStoreId(defaultStore.getId());
        airbnbChannel.setColor("#F56C6C");
        airbnbChannel.setEnabled(true);
        airbnbChannel.setDescription("Airbnb");

        Channel agodaChannel = new Channel("阿凡达", "AGODA", ChannelType.OTA);
        agodaChannel.setUser(defaultUser);
        agodaChannel.setStoreId(defaultStore.getId());
        agodaChannel.setColor("#E6A23C");
        agodaChannel.setEnabled(true);
        agodaChannel.setDescription("Agoda");

        Channel expediadChannel = new Channel("Expedia", "EXPEDIA", ChannelType.OTA);
        expediadChannel.setUser(defaultUser);
        expediadChannel.setStoreId(defaultStore.getId());
        expediadChannel.setColor("#303133");
        expediadChannel.setEnabled(true);
        expediadChannel.setDescription("Expedia");

        Channel bookingChannel = new Channel("Booking.com", "BOOKING", ChannelType.OTA);
        bookingChannel.setUser(defaultUser);
        bookingChannel.setStoreId(defaultStore.getId());
        bookingChannel.setColor("#003580");
        bookingChannel.setEnabled(true);
        bookingChannel.setDescription("Booking.com");

        Channel travelokaChannel = new Channel("Traveloka", "TRAVELOKA", ChannelType.OTA);
        travelokaChannel.setUser(defaultUser);
        travelokaChannel.setStoreId(defaultStore.getId());
        travelokaChannel.setColor("#409EFF");
        travelokaChannel.setEnabled(true);
        travelokaChannel.setDescription("Traveloka");

        Channel tripChannel = new Channel("Trip.com", "TRIP", ChannelType.OTA);
        tripChannel.setUser(defaultUser);
        tripChannel.setStoreId(defaultStore.getId());
        tripChannel.setColor("#409EFF");
        tripChannel.setEnabled(true);
        tripChannel.setDescription("Trip.com");

        Channel bookingEngineChannel = new Channel("预订引擎", "BOOKING_ENGINE", ChannelType.DIRECT);
        bookingEngineChannel.setUser(defaultUser);
        bookingEngineChannel.setStoreId(defaultStore.getId());
        bookingEngineChannel.setColor("#409EFF");
        bookingEngineChannel.setEnabled(true);
        bookingEngineChannel.setDescription("在线预订引擎");

        Channel tiketChannel = new Channel("Tiket.com", "TIKET", ChannelType.OTA);
        tiketChannel.setUser(defaultUser);
        tiketChannel.setStoreId(defaultStore.getId());
        tiketChannel.setColor("#409EFF");
        tiketChannel.setEnabled(true);
        tiketChannel.setDescription("Tiket.com");

        Channel chineseWebChannel = new Channel("中文网站", "CHINESE_WEB", ChannelType.DIRECT);
        chineseWebChannel.setUser(defaultUser);
        chineseWebChannel.setStoreId(defaultStore.getId());
        chineseWebChannel.setColor("#67C23A");
        chineseWebChannel.setEnabled(true);
        chineseWebChannel.setDescription("中文官方网站");

        Channel rednotChannel = new Channel("红注", "REDNOT", ChannelType.OTA);
        rednotChannel.setUser(defaultUser);
        rednotChannel.setStoreId(defaultStore.getId());
        rednotChannel.setColor("#F56C6C");
        rednotChannel.setEnabled(true);
        rednotChannel.setDescription("红注");

        Channel neppanChannel = new Channel("尼潘", "NEPPAN", ChannelType.OTA);
        neppanChannel.setUser(defaultUser);
        neppanChannel.setStoreId(defaultStore.getId());
        neppanChannel.setColor("#E6A23C");
        neppanChannel.setEnabled(true);
        neppanChannel.setDescription("尼潘");

        Channel hostelWorldChannel = new Channel("青年旅舍世界", "HOSTELWORLD", ChannelType.OTA);
        hostelWorldChannel.setUser(defaultUser);
        hostelWorldChannel.setStoreId(defaultStore.getId());
        hostelWorldChannel.setColor("#E6A23C");
        hostelWorldChannel.setEnabled(true);
        hostelWorldChannel.setDescription("青年旅舍世界");

        Channel tujiaChannel = new Channel("途家", "TUJIA", ChannelType.OTA);
        tujiaChannel.setUser(defaultUser);
        tujiaChannel.setStoreId(defaultStore.getId());
        tujiaChannel.setColor("#E6A23C");
        tujiaChannel.setEnabled(true);
        tujiaChannel.setDescription("途家民宿");

        channelRepository.save(directChannel);
        channelRepository.save(manualChannel);
        channelRepository.save(airbnbChannel);
        channelRepository.save(agodaChannel);
        channelRepository.save(expediadChannel);
        channelRepository.save(bookingChannel);
        channelRepository.save(travelokaChannel);
        channelRepository.save(tripChannel);
        channelRepository.save(bookingEngineChannel);
        channelRepository.save(tiketChannel);
        channelRepository.save(chineseWebChannel);
        channelRepository.save(rednotChannel);
        channelRepository.save(neppanChannel);
        channelRepository.save(hostelWorldChannel);
        channelRepository.save(tujiaChannel);

        // 初始化一些预订数据
        // 先删除旧的测试预订数据,避免重复
        System.out.println("===== 清理旧预订数据 =====");
        long beforeCount = reservationRepository.count();
        System.out.println("删除前预订数量: " + beforeCount);
        reservationRepository.deleteAll();
        long afterCount = reservationRepository.count();
        System.out.println("删除后预订数量: " + afterCount);
        System.out.println("==========================\n");

        Room room1 = roomRepository.findByRoomNumber("a01").orElse(null);
        if (room1 != null) {
            Reservation reservation1 = new Reservation(
                room1, directChannel, "林",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3),
                2, new BigDecimal("288.00")
            );
            reservation1.setGuestPhone("13800138000");
            reservation1.setOrderNumber("RSV-A01-" + System.currentTimeMillis());
            reservationRepository.save(reservation1);
        }

        Room room2 = roomRepository.findByRoomNumber("a02").orElse(null);
        if (room2 != null) {
            Reservation reservation2 = new Reservation(
                room2, tripChannel, "张三",
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                1, new BigDecimal("268.00")
            );
            reservation2.setGuestPhone("13900139000");
            reservation2.setStatus(ReservationStatus.CHECKED_IN);
            reservation2.setOrderNumber("RSV-A02-" + System.currentTimeMillis());
            reservationRepository.save(reservation2);
        }

        // 初始化快捷回复数据
        if (quickReplyRepository.count() == 0) {
            QuickReply quickReply1 = new QuickReply();
            quickReply1.setUserId(1L);
            quickReply1.setStoreId(defaultStore.getId());
            quickReply1.setTitle("11");
            quickReply1.setMessage("11{Property name}");
            quickReplyRepository.save(quickReply1);

            System.out.println("快捷回复数据初始化完成");
        }

        // 初始化通知设置数据
        if (notificationSettingRepository.count() == 0) {
            NotificationSetting notificationSetting = new NotificationSetting();
            notificationSetting.setUserId(1L);
            notificationSetting.setOrderPopup(true);
            notificationSetting.setOrderSound(true);
            notificationSetting.setChatPopup(true);
            notificationSetting.setChatSound(true);
            notificationSettingRepository.save(notificationSetting);

            System.out.println("通知设置数据初始化完成");
        }

        // 初始化自动化消息数据
        if (autoMessageRepository.count() == 0) {
            AutoMessage autoMessage1 = new AutoMessage();
            autoMessage1.setUserId(1L);
            autoMessage1.setStoreId(defaultStore.getId());
            autoMessage1.setTitle("入住前确认");
            autoMessage1.setMessage("您好,欢迎预订我们的酒店。您的入住时间为明天下午3点,请提前告知我们您的到达时间。");
            autoMessage1.setAutomationRule("入住前24小时");
            autoMessage1.setChannel("Booking.com");
            autoMessage1.setRoom("全部房间");
            autoMessage1.setEnabled(true);
            autoMessageRepository.save(autoMessage1);

            AutoMessage autoMessage2 = new AutoMessage();
            autoMessage2.setUserId(1L);
            autoMessage2.setStoreId(defaultStore.getId());
            autoMessage2.setTitle("入住指南");
            autoMessage2.setMessage("感谢您选择我们!这里是入住指南:1.办理入住时间15:00-22:00 2.退房时间11:00前 3.WiFi密码将在入住时提供");
            autoMessage2.setAutomationRule("入住当天");
            autoMessage2.setChannel("全部渠道");
            autoMessage2.setRoom("全部房间");
            autoMessage2.setEnabled(true);
            autoMessageRepository.save(autoMessage2);

            AutoMessage autoMessage3 = new AutoMessage();
            autoMessage3.setUserId(1L);
            autoMessage3.setStoreId(defaultStore.getId());
            autoMessage3.setTitle("退房提醒");
            autoMessage3.setMessage("温馨提醒:今天是您的退房日,退房时间为上午11点前。如需延迟退房,请提前联系前台。");
            autoMessage3.setAutomationRule("退房当天");
            autoMessage3.setChannel("全部渠道");
            autoMessage3.setRoom("全部房间");
            autoMessage3.setEnabled(false);
            autoMessageRepository.save(autoMessage3);

            System.out.println("自动化消息数据初始化完成");
        }

        // 初始化保洁配置数据
        if (cleaningConfigRepository.count() == 0) {
            // 为默认门店创建保洁配置
            if (defaultStore != null) {
                CleaningConfig cleaningConfig = new CleaningConfig();
                cleaningConfig.setUserId(1L);
                cleaningConfig.setStoreId(defaultStore.getId());
                cleaningConfig.setEnabled(true);
                cleaningConfig.setStayStartTime("12:00");
                cleaningConfig.setStayEndTime("15:00");
                cleaningConfig.setCheckoutStartTime("10:00");
                cleaningConfig.setCheckoutEndTime("16:00");
                cleaningConfig.setAutoStayTask(true);
                cleaningConfig.setAutoCheckoutTask(true);
                cleaningConfigRepository.save(cleaningConfig);

                System.out.println("保洁配置数据初始化完成");
            }
        }

        // 初始化保洁员数据
        if (cleanerRepository.count() == 0) {
            if (defaultStore != null) {
                Cleaner cleaner1 = new Cleaner();
                cleaner1.setUserId(1L);
                cleaner1.setStoreId(defaultStore.getId());
                cleaner1.setName("田中花子");
                cleaner1.setEmail("tanaka.hanako@example.com");
                cleanerRepository.save(cleaner1);

                Cleaner cleaner2 = new Cleaner();
                cleaner2.setUserId(1L);
                cleaner2.setStoreId(defaultStore.getId());
                cleaner2.setName("佐藤太郎");
                cleaner2.setEmail("sato.taro@example.com");
                cleanerRepository.save(cleaner2);

                System.out.println("保洁员数据初始化完成");
            }
        }

        // 初始化易耗品数据
        if (cleaningSupplyRepository.count() == 0) {
            CleaningSupply supply1 = new CleaningSupply();
            supply1.setUserId(1L);
            supply1.setStoreId(defaultStore.getId());
            supply1.setRoomType("大床房");
            supply1.setSupplies("毛巾×2,浴巾×2,牙刷×2,牙膏×2,拖鞋×2,洗发水,沐浴露,护发素");
            cleaningSupplyRepository.save(supply1);

            CleaningSupply supply2 = new CleaningSupply();
            supply2.setUserId(1L);
            supply2.setStoreId(defaultStore.getId());
            supply2.setRoomType("标准间");
            supply2.setSupplies("毛巾×4,浴巾×2,牙刷×2,牙膏×2,拖鞋×2,洗发水,沐浴露,护发素,棉签");
            cleaningSupplyRepository.save(supply2);

            CleaningSupply supply3 = new CleaningSupply();
            supply3.setUserId(1L);
            supply3.setStoreId(defaultStore.getId());
            supply3.setRoomType("套房");
            supply3.setSupplies("毛巾×6,浴巾×4,牙刷×4,牙膏×4,拖鞋×4,洗发水×2,沐浴露×2,护发素×2,棉签,浴袍×2,茶包");
            cleaningSupplyRepository.save(supply3);

            System.out.println("易耗品数据初始化完成");
        }

        System.out.println("测试数据初始化完成");
    }
}