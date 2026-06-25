export type UserRole = 'USER' | 'ADMIN'

export type OAuthProviderCode = 'GOOGLE' | 'KAKAO' | 'NAVER'

export type PropertyType = 'APARTMENT' | 'OFFICETEL' | 'VILLA' | 'HOUSE'

export type TransactionType = 'SALE' | 'JEONSE' | 'MONTHLY_RENT'

export type ShapDirection = 'UP' | 'DOWN' | 'NEUTRAL'

export type AdministrativeClusterLevel = 'LEGAL_DONG' | 'SIGUNGU'

export interface ApiErrorDetail {
  field?: string
  reason: string
}

export interface ApiErrorResponse {
  code: string
  message: string
  details?: ApiErrorDetail[]
  path: string
  timestamp: string
}

export interface PageMetadata {
  number: number
  size: number
  totalElements: number
  totalPages: number
}

export interface PagedResponse<T> {
  items: T[]
  page: PageMetadata
}

export interface AuthUser {
  userId: number
  email: string
  displayName: string
  roles: UserRole[]
}

export interface AdminUserSummary {
  userId: number
  email: string
  displayName?: string | null
  role: UserRole
  enabled: boolean
  lastLoginAt?: string | null
  createdAt: string
  updatedAt: string
}

export interface AdminUserListParams {
  page?: number
  size?: number
  keyword?: string
  role?: UserRole
  enabled?: boolean
  sort?: string
}

export interface AdminUserRoleUpdateRequest {
  role: UserRole
}

export interface AdminUserEnabledUpdateRequest {
  enabled: boolean
}

export interface AdminUserMutationResponse {
  user: AdminUserSummary
  message: string
}

export interface AuthMeResponse {
  authenticated: boolean
  user: AuthUser | null
}

export interface AuthRefreshResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: AuthUser
}

export interface AuthLoginRequest {
  email: string
  password: string
}

export interface AuthSignupRequest extends AuthLoginRequest {
  displayName: string
  birthDate?: string
  phoneNumber?: string
}

export type AuthSessionResponse = AuthRefreshResponse

export interface AuthLogoutResponse {
  message: string
}

export interface UpdateProfileRequest {
  displayName: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export interface DeactivateAccountRequest {
  password: string
}

export interface AccountMutationResponse {
  message: string
}

export interface AccountIdentifierRecoveryRequest {
  displayName: string
}

export interface PasswordRecoveryRequest {
  email: string
}

export interface DirectPasswordResetRequest {
  email: string
  displayName: string
  newPassword: string
}

export interface AccountRecoveryResponse {
  message: string
}

export interface PendingSocialSignupResponse {
  provider: OAuthProviderCode
  email?: string | null
  displayName?: string | null
  matchingEmailAccountExists: boolean
}

export interface SocialSignupRequest extends AuthSignupRequest {}

export interface SocialLinkRequest extends AuthLoginRequest {}

export interface LatestTransaction {
  transactionType: TransactionType
  dealAmount: number
  depositAmount?: number | null
  monthlyRent?: number | null
  dealDate: string
}

export interface PropertyMapItem {
  propertyId: number
  propertyType: PropertyType
  name: string
  address: string
  lat: number
  lng: number
  latestTransaction?: LatestTransaction | null
  dealCount: number
  recentTransactionCount: number
  recentYearAverageDealAmount?: number | null
  recentYearAverageJeonseDepositAmount?: number | null
  priceFilterDimmed?: boolean
  aiAvailable: boolean
}

export interface AdministrativeCluster {
  clusterId: string
  level: AdministrativeClusterLevel
  sido: string
  sigungu: string
  legalDong?: string | null
  label: string
  centerLat: number
  centerLng: number
  propertyCount: number
  transactionCount: number
  averageDealAmount?: number | null
}

export interface Bounds {
  swLat: number
  swLng: number
  neLat: number
  neLng: number
}

export interface MapSearchParams extends Bounds {
  zoomLevel: number
  propertyTypes?: PropertyType[]
  transactionTypes?: TransactionType[]
  minDealAmount?: number
  maxDealAmount?: number
  minSaleAmount?: number
  maxSaleAmount?: number
  minJeonseDepositAmount?: number
  maxJeonseDepositAmount?: number
  minAreaM2?: number
  maxAreaM2?: number
  dealYearFrom?: number
  dealYearTo?: number
}

export interface PropertyMapResponse {
  items: PropertyMapItem[]
  administrativeClusters: AdministrativeCluster[]
  bounds: Bounds
  filters: {
    propertyTypes: PropertyType[]
    transactionTypes: TransactionType[]
    minSaleAmount?: number | null
    maxSaleAmount?: number | null
    minJeonseDepositAmount?: number | null
    maxJeonseDepositAmount?: number | null
    zoomLevel: number
  }
}

export interface PropertySearchParams {
  sido?: string
  sigungu?: string
  legalDong?: string
  complexName?: string
  keyword?: string
  centerLat?: number
  centerLng?: number
  swLat?: number
  swLng?: number
  neLat?: number
  neLng?: number
  propertyTypes?: PropertyType[]
  transactionTypes?: TransactionType[]
  minDealAmount?: number
  maxDealAmount?: number
  minSaleAmount?: number
  maxSaleAmount?: number
  minJeonseDepositAmount?: number
  maxJeonseDepositAmount?: number
  minAreaM2?: number
  maxAreaM2?: number
  dealYearFrom?: number
  dealYearTo?: number
  page?: number
  size?: number
  sort?: string
}

export interface PropertySearchItem {
  propertyId: number
  propertyType: PropertyType
  name: string
  address: string
  legalDong: string
  lat: number
  lng: number
  distanceM?: number
  latestTransaction?: LatestTransaction | null
  aiAvailable: boolean
}

export interface PropertyDetail {
  propertyId: number
  propertyType: PropertyType
  name: string
  address: {
    sido: string
    sigungu: string
    legalDong: string
    roadAddress?: string | null
  }
  location: {
    lat: number
    lng: number
  }
  summary: {
    builtYear?: number | null
    householdCount?: number | null
    latestDealAmount?: number | null
    latestDealDate?: string | null
  }
  transactions: PropertyTransaction[]
  favorite?: {
    apartmentFavorited: boolean
    areaFavorited: boolean
  }
  ai: {
    valuationAvailable: boolean
    shapAvailable: boolean
    unsupportedReason?: string | null
  }
}

export interface PropertyTransaction {
  transactionId?: number
  transactionType: TransactionType
  dealAmount?: number | null
  depositAmount?: number | null
  monthlyRent?: number | null
  dealDate: string
  exclusiveAreaM2?: number | null
  floor?: number | null
}

export interface ValuationRequest {
  exclusiveAreaM2: number
  floor: number
  asOfDate: string
}

export interface ValuationResponse {
  propertyId: number
  supported: boolean
  estimatedPrice?: number
  currency?: 'KRW'
  predictionInterval?: {
    lower: number
    upper: number
  }
  modelVersion?: string
  baselineDate?: string
  featureSetVersion?: string
  message: string
}

export interface ShapRequest extends ValuationRequest {}

export interface ShapValue {
  feature: string
  labelKo: string
  value: string | number
  shapValue: number
  direction: ShapDirection
}

export interface ShapResponse {
  propertyId: number
  supported: boolean
  baseValue?: number
  prediction?: number
  currency?: 'KRW'
  values: ShapValue[]
  modelVersion?: string
  baselineDate?: string
  featureSetVersion?: string
  message: string
}

export interface NewApartmentAnalysisRequest {
  propertyName: string
  sido: string
  sigungu: string
  legalDong: string
  latitude?: number | null
  longitude?: number | null
  householdCount?: number | null
  exclusiveAreaM2: number
  floor: number
  topFloor?: number | null
  builtYear: number
  asOfDate: string
  distanceToStationM?: number | null
}

export interface NewApartmentAddressCandidate {
  fullAddress: string
  roadAddress?: string | null
  jibunAddress?: string | null
  sido: string
  sigungu: string
  legalDong: string
  latitude: number
  longitude: number
}

export interface NewApartmentAnalysisResponse {
  propertyName: string
  valuation: ValuationResponse
  shap: ShapResponse
  message: string
}

export interface ChatRequest {
  question: string
  runtimeContext?: Record<string, unknown>
}

export interface ChatContext {
  source: string
  text: string
}

export interface ChatResponse {
  available: boolean
  answer: string
  contexts: ChatContext[]
  model: string
  ragConfig: {
    embedding: string
    chunkSize: number
    overlap: number
    hybrid: boolean
    rerank: boolean
  }
}

export interface NewsFeedItem {
  title: string
  summary: string
  link: string
  originalLink?: string | null
  publishedAt?: string | null
  source: string
}

export interface NewsFeedResponse {
  available: boolean
  keyword: string
  message: string
  items: NewsFeedItem[]
}

export interface NewsSearchParams {
  query?: string
  display?: number
}

export interface FavoriteApartmentItem {
  favoriteId: number
  propertyId: number
  propertyType: PropertyType
  name: string
  address: string
  lat: number
  lng: number
  latestTransaction?: LatestTransaction | null
  createdAt: string
}

export interface FavoriteAreaItem {
  favoriteAreaId: number
  label: string
  sido?: string
  sigungu?: string
  legalDong?: string
  centerLat?: number
  centerLng?: number
  zoomLevel?: number
  createdAt: string
}

export interface FavoriteApartmentCreateResponse {
  favoriteId: number
  propertyId: number
  createdAt: string
  message: string
}

export interface FavoriteApartmentDeleteResponse {
  propertyId: number
  message: string
}

export interface FavoriteAreaCreateRequest {
  label: string
  sido?: string
  sigungu?: string
  legalDong?: string
  centerLat?: number
  centerLng?: number
  zoomLevel?: number
}

export interface FavoriteAreaCreateResponse {
  favoriteAreaId: number
  label: string
  createdAt: string
  message: string
}

export interface FavoriteAreaDeleteResponse {
  favoriteAreaId: number
  message: string
}

export type CommunityCategory = 'NOTICE' | 'FREE' | 'DEAL_REVIEW' | 'QNA'

export interface CommunityPostSummary {
  postId: number
  category: CommunityCategory
  title: string
  authorUserId?: number | null
  authorDisplayName?: string | null
  viewCount: number
  commentCount: number
  createdAt: string
  updatedAt: string
}

export interface CommunityComment {
  commentId: number
  postId: number
  parentCommentId?: number | null
  authorUserId?: number | null
  authorDisplayName?: string | null
  content: string
  createdAt: string
  updatedAt: string
  replies: CommunityComment[]
}

export interface CommunityPostDetail extends CommunityPostSummary {
  content: string
  relatedPropertyId?: number | null
  relatedPropertyName?: string | null
  relatedPropertyAddress?: string | null
  comments: CommunityComment[]
}

export interface CommunityPostListParams {
  page?: number
  size?: number
  sort?: 'createdAt,desc' | 'viewCount,desc' | 'commentCount,desc'
  keyword?: string
  category?: CommunityCategory
}

export interface CommunityPostCreateRequest {
  category: CommunityCategory
  title: string
  content: string
  relatedPropertyId?: number | null
}

export interface CommunityPostUpdateRequest extends CommunityPostCreateRequest {}

export interface CommunityCommentCreateRequest {
  parentCommentId?: number | null
  content: string
}

export interface CommunityCommentUpdateRequest {
  content: string
}

export interface CommunityMutationResponse {
  id: number
  message: string
}
