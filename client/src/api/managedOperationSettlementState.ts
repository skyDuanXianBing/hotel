export interface ManagedOperationFileIdentity {
  name: string
  size: number
  lastModified: number
}

export const managedOperationFileFingerprint = (
  file: ManagedOperationFileIdentity | null,
  revision: number,
) => ({
  identity: file ? `${file.name}:${file.size}:${file.lastModified}` : '',
  revision,
})
