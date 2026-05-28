import type { FacilityDTO } from '@/api/roomType'

export interface RoomFacilityItem {
  key: string
  label: string
  labelKey: string
  payload: FacilityDTO
}

export interface RoomFacilitySection {
  title: string
  titleKey: string
  items: RoomFacilityItem[]
}

export const STORE_FACILITY_OPTIONS: Array<{
  label: string
  labelKey: string
  payload: FacilityDTO
}> = [
  { label: 'WiFi', labelKey: 'stage6.constants.facilities.store.wifi', payload: { group: 'Facilities', name: 'WiFi' } },
  { label: 'Parking', labelKey: 'stage6.constants.facilities.store.parking', payload: { group: 'Facilities', name: 'Parking' } },
  { label: 'Elevator', labelKey: 'stage6.constants.facilities.store.elevator', payload: { group: 'Facilities', name: 'Elevator' } },
  { label: 'Luggage Storage', labelKey: 'stage6.constants.facilities.store.luggageStorage', payload: { group: 'Services', name: 'Luggage Storage' } },
  { label: 'Gym', labelKey: 'stage6.constants.facilities.store.gym', payload: { group: 'Facilities', name: 'Gym' } },
  { label: 'Swimming Pool', labelKey: 'stage6.constants.facilities.store.swimmingPool', payload: { group: 'Facilities', name: 'Swimming Pool' } },
  { label: 'Outdoor Pool', labelKey: 'stage6.constants.facilities.store.outdoorPool', payload: { group: 'Facilities', name: 'Outdoor Pool' } },
  { label: 'Restaurant', labelKey: 'stage6.constants.facilities.store.restaurant', payload: { group: 'Dining', name: 'Restaurant' } },
  { label: 'Lounge', labelKey: 'stage6.constants.facilities.store.lounge', payload: { group: 'Facilities', name: 'Lounge' } },
  { label: 'Airport Shuttle', labelKey: 'stage6.constants.facilities.store.airportShuttle', payload: { group: 'Transport', name: 'Airport Shuttle' } },
  { label: 'Bar', labelKey: 'stage6.constants.facilities.store.bar', payload: { group: 'Dining', name: 'Bar' } },
  { label: 'Front Desk', labelKey: 'stage6.constants.facilities.store.frontDesk', payload: { group: 'Services', name: 'Front Desk' } },
  { label: 'Meeting Room', labelKey: 'stage6.constants.facilities.store.meetingRoom', payload: { group: 'Business', name: 'Meeting Room' } },
  { label: 'Laundry Service', labelKey: 'stage6.constants.facilities.store.laundryService', payload: { group: 'Services', name: 'Laundry Service' } },
  { label: 'Sauna', labelKey: 'stage6.constants.facilities.store.sauna', payload: { group: 'Wellness', name: 'Sauna' } },
  { label: 'Spa', labelKey: 'stage6.constants.facilities.store.spa', payload: { group: 'Wellness', name: 'Spa' } },
  { label: 'Concierge', labelKey: 'stage6.constants.facilities.store.concierge', payload: { group: 'Services', name: 'Concierge' } },
  { label: 'ATM', labelKey: 'stage6.constants.facilities.store.atm', payload: { group: 'Facilities', name: 'ATM' } },
  { label: 'Currency Exchange', labelKey: 'stage6.constants.facilities.store.currencyExchange', payload: { group: 'Services', name: 'Currency Exchange' } },
  { label: 'Breakfast Available', labelKey: 'stage6.constants.facilities.store.breakfastAvailable', payload: { group: 'Dining', name: 'Breakfast Available' } },
  { label: 'Gift Shop', labelKey: 'stage6.constants.facilities.store.giftShop', payload: { group: 'Facilities', name: 'Gift Shop' } },
  { label: 'Printer', labelKey: 'stage6.constants.facilities.store.printer', payload: { group: 'Business', name: 'Printer' } },
  { label: 'Room Service', labelKey: 'stage6.constants.facilities.store.roomService', payload: { group: 'Services', name: 'Room Service' } },
  { label: 'Extra Bed / Crib', labelKey: 'stage6.constants.facilities.store.extraBedCrib', payload: { group: 'Family', name: 'Extra Bed / Crib' } },
  { label: '24-hour Front Desk', labelKey: 'stage6.constants.facilities.store.frontDesk24h', payload: { group: 'Services', name: '24-hour Front Desk' } },
  { label: 'Non-smoking', labelKey: 'stage6.constants.facilities.store.nonSmoking', payload: { group: 'Policies', name: 'Non-smoking' } },
  { label: 'Accessible Entrance', labelKey: 'stage6.constants.facilities.store.accessibleEntrance', payload: { group: 'Accessibility', name: 'Accessible Entrance' } },
  { label: 'Wheelchair Access', labelKey: 'stage6.constants.facilities.store.wheelchairAccess', payload: { group: 'Accessibility', name: 'Wheelchair Access' } },
  { label: 'Driver Service', labelKey: 'stage6.constants.facilities.store.driverService', payload: { group: 'Transport', name: 'Driver Service' } },
  { label: 'Private Courtyard', labelKey: 'stage6.constants.facilities.store.privateCourtyard', payload: { group: 'Facilities', name: 'Private Courtyard' } },
  { label: 'Multilingual Staff', labelKey: 'stage6.constants.facilities.store.multilingualStaff', payload: { group: 'Services', name: 'Multilingual Staff' } },
]

export const ROOM_FACILITY_SECTIONS: RoomFacilitySection[] = [
  {
    title: 'Popular Amenities',
    titleKey: 'stage6.constants.facilities.sections.popular',
    items: [
      { key: 'airConditioning', label: 'Air Conditioning', labelKey: 'stage6.constants.facilities.room.airConditioning', payload: { group: 'Popular', name: 'Air Conditioning' } },
      { key: 'heating', label: 'Heating', labelKey: 'stage6.constants.facilities.room.heating', payload: { group: 'Popular', name: 'Heating' } },
      { key: 'noSmoking', label: 'Non-smoking', labelKey: 'stage6.constants.facilities.room.noSmoking', payload: { group: 'Popular', name: 'Non-smoking' } },
      { key: 'tv', label: 'TV', labelKey: 'stage6.constants.facilities.room.tv', payload: { group: 'Popular', name: 'TV' } },
      { key: 'wifi', label: 'WiFi', labelKey: 'stage6.constants.facilities.room.wifi', payload: { group: 'Popular', name: 'WiFi' } },
    ],
  },
  {
    title: 'Bathroom Amenities',
    titleKey: 'stage6.constants.facilities.sections.bathroom',
    items: [
      { key: 'bathtub', label: 'Bathtub', labelKey: 'stage6.constants.facilities.room.bathtub', payload: { group: 'Bathroom', name: 'Bathtub' } },
      { key: 'toiletries', label: 'Cleaning Products', labelKey: 'stage6.constants.facilities.room.toiletries', payload: { group: 'Bathroom', name: 'Cleaning Products' } },
      { key: 'towel', label: 'Towels', labelKey: 'stage6.constants.facilities.room.towel', payload: { group: 'Bathroom', name: 'Towels' } },
      { key: 'shower', label: 'Shower', labelKey: 'stage6.constants.facilities.room.shower', payload: { group: 'Bathroom', name: 'Shower' } },
      { key: 'hairDryer', label: 'Hair Dryer', labelKey: 'stage6.constants.facilities.room.hairDryer', payload: { group: 'Bathroom', name: 'Hair Dryer' } },
      { key: 'slippers', label: 'Slippers', labelKey: 'stage6.constants.facilities.room.slippers', payload: { group: 'Bathroom', name: 'Slippers' } },
      { key: 'separateShower', label: 'Separate Shower and Bathtub', labelKey: 'stage6.constants.facilities.room.separateShower', payload: { group: 'Bathroom', name: 'Separate Shower and Bathtub' } },
      { key: 'toiletries2', label: 'Toiletries', labelKey: 'stage6.constants.facilities.room.toiletries2', payload: { group: 'Bathroom', name: 'Toiletries' } },
    ],
  },
  {
    title: 'Dining Amenities',
    titleKey: 'stage6.constants.facilities.sections.dining',
    items: [
      { key: 'teaCoffee', label: 'Tea/Coffee Maker', labelKey: 'stage6.constants.facilities.room.teaCoffee', payload: { group: 'Dining', name: 'Tea/Coffee Maker' } },
      { key: 'tableware', label: 'Tableware', labelKey: 'stage6.constants.facilities.room.tableware', payload: { group: 'Dining', name: 'Tableware' } },
      { key: 'dinnerware', label: 'Dinnerware', labelKey: 'stage6.constants.facilities.room.dinnerware', payload: { group: 'Dining', name: 'Dinnerware' } },
      { key: 'iceBox', label: 'Refrigerator', labelKey: 'stage6.constants.facilities.room.iceBox', payload: { group: 'Dining', name: 'Refrigerator' } },
      { key: 'grill', label: 'Oven', labelKey: 'stage6.constants.facilities.room.grill', payload: { group: 'Dining', name: 'Oven' } },
      { key: 'wine', label: 'Wine Glasses', labelKey: 'stage6.constants.facilities.room.wine', payload: { group: 'Dining', name: 'Wine Glasses' } },
      { key: 'service24h', label: '24-hour Room Service', labelKey: 'stage6.constants.facilities.room.service24h', payload: { group: 'Dining', name: '24-hour Room Service' } },
      { key: 'hotWater', label: 'Electric Kettle', labelKey: 'stage6.constants.facilities.room.hotWater', payload: { group: 'Dining', name: 'Electric Kettle' } },
      { key: 'microwave', label: 'Minibar', labelKey: 'stage6.constants.facilities.room.microwave', payload: { group: 'Dining', name: 'Minibar' } },
      { key: 'coffeeService', label: 'Coffee and Tea Facilities', labelKey: 'stage6.constants.facilities.room.coffeeService', payload: { group: 'Dining', name: 'Coffee and Tea Facilities' } },
      { key: 'stove', label: 'Stove', labelKey: 'stage6.constants.facilities.room.stove', payload: { group: 'Dining', name: 'Stove' } },
      { key: 'cutlery', label: 'Cutlery', labelKey: 'stage6.constants.facilities.room.cutlery', payload: { group: 'Dining', name: 'Cutlery' } },
    ],
  },
  {
    title: 'Entertainment',
    titleKey: 'stage6.constants.facilities.sections.entertainment',
    items: [
      { key: 'newspaper', label: 'Newspaper', labelKey: 'stage6.constants.facilities.room.newspaper', payload: { group: 'Entertainment', name: 'Newspaper' } },
      { key: 'cableTv', label: 'Pay TV', labelKey: 'stage6.constants.facilities.room.cableTv', payload: { group: 'Entertainment', name: 'Pay TV' } },
      { key: 'satellite', label: 'Satellite Channels', labelKey: 'stage6.constants.facilities.room.satellite', payload: { group: 'Entertainment', name: 'Satellite Channels' } },
      { key: 'phone', label: 'Telephone', labelKey: 'stage6.constants.facilities.room.phone', payload: { group: 'Entertainment', name: 'Telephone' } },
      { key: 'radio', label: 'Radio', labelKey: 'stage6.constants.facilities.room.radio', payload: { group: 'Entertainment', name: 'Radio' } },
    ],
  },
  {
    title: 'More Amenities',
    titleKey: 'stage6.constants.facilities.sections.more',
    items: [
      { key: 'wardrobe', label: 'Wardrobe', labelKey: 'stage6.constants.facilities.room.wardrobe', payload: { group: 'More', name: 'Wardrobe' } },
      { key: 'dryer', label: 'Dryer', labelKey: 'stage6.constants.facilities.room.dryer', payload: { group: 'More', name: 'Dryer' } },
      { key: 'rack', label: 'Clothes Rack', labelKey: 'stage6.constants.facilities.room.rack', payload: { group: 'More', name: 'Clothes Rack' } },
      { key: 'mosquito', label: 'Mosquito Net', labelKey: 'stage6.constants.facilities.room.mosquito', payload: { group: 'More', name: 'Mosquito Net' } },
      { key: 'washer', label: 'Washing Machine', labelKey: 'stage6.constants.facilities.room.washer', payload: { group: 'More', name: 'Washing Machine' } },
      { key: 'curtain', label: 'Blackout Curtains', labelKey: 'stage6.constants.facilities.room.curtain', payload: { group: 'More', name: 'Blackout Curtains' } },
      { key: 'toys', label: 'Crib Available', labelKey: 'stage6.constants.facilities.room.toys', payload: { group: 'Family', name: 'Crib Available' } },
      { key: 'board', label: 'Baby Bath and High Chair', labelKey: 'stage6.constants.facilities.room.board', payload: { group: 'Family', name: 'Baby Bath and High Chair' } },
    ],
  },
]
