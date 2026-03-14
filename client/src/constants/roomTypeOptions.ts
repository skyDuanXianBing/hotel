export interface RoomTypeOption {
  label: string
  value: string
}

export const ROOM_SIZE_UNIT_OPTIONS: RoomTypeOption[] = [
  { label: '平方米 (sqm)', value: 'sqm' },
  { label: '平方英尺 (sqft)', value: 'sqft' },
]

export const ROOM_TYPE_OPTIONS: RoomTypeOption[] = [
  { label: '1 - Apartment', value: '1' },
  { label: '4 - Quadruple', value: '4' },
  { label: '5 - Suite', value: '5' },
  { label: '7 - Triple', value: '7' },
  { label: '8 - Twin', value: '8' },
  { label: '9 - Double', value: '9' },
  { label: '10 - Single', value: '10' },
  { label: '12 - Studio', value: '12' },
  { label: '13 - Family', value: '13' },
  { label: '25 - Dormitory room', value: '25' },
  { label: '26 - Bed in Dormitory', value: '26' },
  { label: '27 - Bungalow', value: '27' },
  { label: '28 - Chalet', value: '28' },
  { label: '29 - Holiday home', value: '29' },
  { label: '31 - Villa', value: '31' },
  { label: '32 - Mobile home', value: '32' },
  { label: '33 - Tent', value: '33' },
  { label: '34 - Powered/Unpowered Site', value: '34' },
  { label: '35 - King', value: '35' },
  { label: '36 - Queen', value: '36' },
]

export const LEGACY_ROOM_TYPE_CODE_MAP: Record<string, string> = {
  Apartment: '1',
  Quadruple: '4',
  Suite: '5',
  Triple: '7',
  Twin: '8',
  Double: '9',
  Single: '10',
  Studio: '12',
  Family: '13',
  'Dormitory Room': '25',
  'Dormitory room': '25',
  'Bed In Dormitory': '26',
  'Bed in Dormitory': '26',
  Bungalow: '27',
  Chalet: '28',
  'Holiday Home': '29',
  'Holiday home': '29',
  Villa: '31',
  'Mobile Home': '32',
  'Mobile home': '32',
  Tent: '33',
  'Powered/Unpowered Site': '34',
  King: '35',
  Queen: '36',
}
