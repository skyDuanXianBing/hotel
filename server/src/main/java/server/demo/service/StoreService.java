package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.Store;
import server.demo.entity.StorePolicy;
import server.demo.repository.StorePolicyRepository;
import server.demo.repository.StoreRepository;

import java.util.List;
import java.util.Optional;

@Service
public class StoreService {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StorePolicyRepository storePolicyRepository;

    /**
     * 获取所有门店
     */
    public List<Store> getAllStores() {
        return storeRepository.findAll();
    }

    /**
     * 根据ID获取门店
     */
    public Optional<Store> getStoreById(Long id) {
        return storeRepository.findById(id);
    }

    /**
     * 创建门店
     */
    @Transactional
    public Store createStore(Store store) {
        return storeRepository.save(store);
    }

    /**
     * 更新门店
     */
    @Transactional
    public Store updateStore(Long id, Store storeDetails) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("门店不存在"));

        // 更新基本信息
        store.setName(storeDetails.getName());
        store.setPhone(storeDetails.getPhone());
        store.setType(storeDetails.getType());
        store.setTimezone(storeDetails.getTimezone());
        store.setManager(storeDetails.getManager());
        store.setOwnerEmail(storeDetails.getOwnerEmail());
        store.setAddress(storeDetails.getAddress());
        store.setCity(storeDetails.getCity());
        store.setState(storeDetails.getState());
        store.setCountry(storeDetails.getCountry());
        store.setCurrency(storeDetails.getCurrency());
        store.setDescription(storeDetails.getDescription());
        store.setEmail(storeDetails.getEmail());
        store.setWechat(storeDetails.getWechat());
        store.setWhatsapp(storeDetails.getWhatsapp());
        store.setLine(storeDetails.getLine());
        store.setLanguage(storeDetails.getLanguage());
        store.setLogo(storeDetails.getLogo());

        return storeRepository.save(store);
    }

    /**
     * 删除门店
     */
    @Transactional
    public void deleteStore(Long id) {
        storeRepository.deleteById(id);
    }

    /**
     * 获取门店政策
     */
    public Optional<StorePolicy> getStorePolicy(Long storeId) {
        return storePolicyRepository.findByStoreId(storeId);
    }

    /**
     * 更新或创建门店政策
     */
    @Transactional
    public StorePolicy saveStorePolicy(Long storeId, StorePolicy policyDetails) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("门店不存在"));

        Optional<StorePolicy> existingPolicy = storePolicyRepository.findByStoreId(storeId);

        StorePolicy policy;
        if (existingPolicy.isPresent()) {
            policy = existingPolicy.get();
        } else {
            policy = new StorePolicy();
            policy.setStore(store);
        }

        policy.setCheckinTime(policyDetails.getCheckinTime());
        policy.setCheckoutTime(policyDetails.getCheckoutTime());
        policy.setChildPolicy(policyDetails.getChildPolicy());
        policy.setSmokingPolicy(policyDetails.getSmokingPolicy());
        policy.setPetPolicy(policyDetails.getPetPolicy());
        policy.setAdditionalRules(policyDetails.getAdditionalRules());
        policy.setHotelTerms(policyDetails.getHotelTerms());

        return storePolicyRepository.save(policy);
    }
}
