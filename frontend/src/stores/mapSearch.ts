import { defineStore } from 'pinia'
import { ref } from 'vue'

import type { AdministrativeCluster, PropertyMapItem, PropertyType } from '@/api/types'
import type { LatLngPoint, MapViewport } from '@/map/kakaoMap'

export interface MapSearchSnapshot {
  selectedPropertyTypes: PropertyType[]
  salePriceMin: number
  salePriceMax: number
  jeonsePriceMin: number
  jeonsePriceMax: number
  zoomLevel: number
  currentViewport: MapViewport
  searchKeyword: string
  activeSearchKeyword: string
  keywordSearchPage: number
  keywordSearchTotal: number | null
  showAdministrativePriceLayer: boolean
  selectedPropertyId: number | null
  mapFocusTarget: LatLngPoint | null
  items: PropertyMapItem[]
  administrativeClusters: AdministrativeCluster[]
}

export const useMapSearchStore = defineStore('mapSearch', () => {
  const snapshot = ref<MapSearchSnapshot | null>(null)

  function save(nextSnapshot: MapSearchSnapshot) {
    snapshot.value = {
      ...nextSnapshot,
      selectedPropertyTypes: [...nextSnapshot.selectedPropertyTypes],
      currentViewport: { ...nextSnapshot.currentViewport },
      mapFocusTarget: nextSnapshot.mapFocusTarget ? { ...nextSnapshot.mapFocusTarget } : null,
      items: [...nextSnapshot.items],
      administrativeClusters: [...nextSnapshot.administrativeClusters]
    }
  }

  function clear() {
    snapshot.value = null
  }

  return {
    snapshot,
    save,
    clear
  }
})
