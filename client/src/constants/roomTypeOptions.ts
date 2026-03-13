export interface RoomTypeOption {
  label: string
  value: string
}

export const ROOM_SIZE_UNIT_OPTIONS: RoomTypeOption[] = [
  { label: '平方米（sqm）', value: 'sqm' },
  { label: '平方英尺（sqft）', value: 'sqft' },
]
