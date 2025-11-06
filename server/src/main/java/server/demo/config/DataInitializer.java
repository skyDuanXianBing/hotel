package server.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.enums.ChannelType;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomStatus;
import server.demo.repository.ChannelRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

// 暂时禁用数据初始化器,因为现在使用多租户模式,每个用户应该创建自己的数据
// @Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Override
    public void run(String... args) throws Exception {
        // 检查是否已有数据
        if (roomTypeRepository.count() > 0) {
            return;
        }

        // 初始化房型数据
        RoomType daBedRoom = new RoomType("大床房", "DBF", 10, "舒适大床房");
        RoomType standardRoom = new RoomType("标准间", "BZJ", 15, "标准双人间");
        RoomType suite = new RoomType("套房", "TF", 5, "豪华套房");

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
        directChannel.setColor("#409EFF");
        directChannel.setEnabled(true);
        directChannel.setDescription("直接预订客户");

        Channel ctripChannel = new Channel("携程", "CTRIP", ChannelType.OTA);
        ctripChannel.setColor("#1890FF");
        ctripChannel.setEnabled(true);
        ctripChannel.setDescription("携程网");

        Channel meituanChannel = new Channel("美团", "MEITUAN", ChannelType.OTA);
        meituanChannel.setColor("#FFB800");
        meituanChannel.setEnabled(true);
        meituanChannel.setDescription("美团");

        Channel fliggyChannel = new Channel("飞猪", "FLIGGY", ChannelType.OTA);
        fliggyChannel.setColor("#FF6A00");
        fliggyChannel.setEnabled(true);
        fliggyChannel.setDescription("飞猪旅行");

        Channel qunarChannel = new Channel("去哪儿", "QUNAR", ChannelType.OTA);
        qunarChannel.setColor("#00C1DE");
        qunarChannel.setEnabled(true);
        qunarChannel.setDescription("去哪儿网");

        Channel bookingChannel = new Channel("Booking.com", "BOOKING", ChannelType.OTA);
        bookingChannel.setColor("#003580");
        bookingChannel.setEnabled(true);
        bookingChannel.setDescription("Booking.com");

        channelRepository.save(directChannel);
        channelRepository.save(ctripChannel);
        channelRepository.save(meituanChannel);
        channelRepository.save(fliggyChannel);
        channelRepository.save(qunarChannel);
        channelRepository.save(bookingChannel);

        // 初始化一些预订数据
        Room room1 = roomRepository.findByRoomNumber("a01").orElse(null);
        if (room1 != null) {
            Reservation reservation1 = new Reservation(
                room1, directChannel, "林",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3),
                2, new BigDecimal("288.00")
            );
            reservation1.setGuestPhone("13800138000");
            reservation1.setOrderNumber("aa-a01");
            reservationRepository.save(reservation1);
        }

        Room room2 = roomRepository.findByRoomNumber("a02").orElse(null);
        if (room2 != null) {
            Reservation reservation2 = new Reservation(
                room2, ctripChannel, "张三",
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                1, new BigDecimal("268.00")
            );
            reservation2.setGuestPhone("13900139000");
            reservation2.setStatus(ReservationStatus.CHECKED_IN);
            reservationRepository.save(reservation2);
        }

        System.out.println("测试数据初始化完成");
    }
}