package com.example.service;

import com.example.dto.InnerCategoryDTO;
import com.example.entity.CategoryEntity;
import com.example.entity.InnerCategoryEntity;
import com.example.exceptions.CategoryNotFoundException;
import com.example.exceptions.InnerCategoryNameFoundException;
import com.example.repository.CategoryRepository;
import com.example.repository.InnerCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Component
public class InnerCategoryService {
    private final CategoryRepository categoryRepository;
    private final InnerCategoryRepository innerCategoryRepository;

    public InnerCategoryService(InnerCategoryRepository innerCategoryRepository, CategoryRepository categoryRepository) {
        this.innerCategoryRepository = innerCategoryRepository;
        this.categoryRepository = categoryRepository;
    }

    public void create(String categoryName, String nameUz, String nameRu) {
        InnerCategoryEntity innerCategory = new InnerCategoryEntity();
        innerCategory.setCategoryId(getIdByCategoryName(categoryName));

        Optional<InnerCategoryEntity> byNameUz = innerCategoryRepository.findByNameUz(nameUz);
        if (byNameUz.isPresent()) {
            throw new InnerCategoryNameFoundException(nameUz + " already exists");
        }
        innerCategory.setNameUz(nameUz);

        Optional<InnerCategoryEntity> byNameRu = innerCategoryRepository.findByNameRu(nameRu);
        if (byNameRu.isPresent()) {
            throw new InnerCategoryNameFoundException(nameUz + " already exists");
        }
        innerCategory.setNameRu(nameRu);

        innerCategoryRepository.save(innerCategory);
    }

    public List<InnerCategoryDTO> getAllUz(String categoryName) {
        List<InnerCategoryDTO> result = new LinkedList<>();

        List<InnerCategoryEntity> byCategoryId = innerCategoryRepository.findByCategoryId(getIdByCategoryName(categoryName));
        for (InnerCategoryEntity innerCategory : byCategoryId) {
            InnerCategoryDTO innerCategoryDTO = new InnerCategoryDTO();
            innerCategoryDTO.setId(innerCategory.getId());
            innerCategoryDTO.setName(innerCategory.getNameUz());
            result.add(innerCategoryDTO);
        }
        return result;
    }

    public List<InnerCategoryDTO> getAllRu(String categoryName) {
        List<InnerCategoryDTO> result = new LinkedList<>();

        List<InnerCategoryEntity> byCategoryId = innerCategoryRepository.findByCategoryId(getIdByCategoryName(categoryName));
        for (InnerCategoryEntity innerCategory : byCategoryId) {
            InnerCategoryDTO innerCategoryDTO = new InnerCategoryDTO();
            innerCategoryDTO.setId(innerCategory.getId());
            innerCategoryDTO.setName(innerCategory.getNameRu());
            result.add(innerCategoryDTO);
        }
        return result;
    }

    public String getInnerCategoryIdByName(String innerCategoryName) {
        Optional<InnerCategoryEntity> byName = innerCategoryRepository.findByName(innerCategoryName);
        if (byName.isEmpty()) {
            throw new CategoryNotFoundException("Category not found by this name: " + innerCategoryName);
        }
        InnerCategoryEntity category = byName.get();

        return category.getId();
    }

    public Boolean findByName(String name) {
        Optional<InnerCategoryEntity> byName = innerCategoryRepository.findByName(name);
        if (byName.isEmpty()) {
            return false;
        }
        InnerCategoryEntity category = byName.get();

        return true;
    }

    public Long getIdByCategoryName(String categoryName) {
        Optional<CategoryEntity> byName = categoryRepository.findByName(categoryName);
        if (byName.isEmpty()) {
            throw new CategoryNotFoundException("Category not found by this name: " + categoryName);
        }
        CategoryEntity category = byName.get();

        return category.getId();
    }

    public void init() {
        create("Xizmatlar", "Qurilish va ta'mirlash", "Строительство и ремонт");
        create("Xizmatlar", "Mebel va gilamlar tozalash", "Чистка мебели и ковров");
        create("Xizmatlar", "Santexnika xizmati", "Сантехнические услуги");
        create("Xizmatlar", "Foto va video xizmatlari", "Фото и видео услуги");
        create("Xizmatlar", "Kompyuter va Kompyuter jihozlarini ta'mirlash", "Ремонт компьютеров и компьютерной техники");
        create("Xizmatlar", "To'y va ma'rosimlar tashkillashtirish", "Организация свадеб и торжеств");
        create("Xizmatlar", "Kamera va signalizatsiya o'rnatish va ta'mirlash", "Установка и ремонт камер и сигнализаций");
        create("Xizmatlar", "Tom orqa va yerlarga ishlov berish", "Кровля и земляные работы");
        create("Xizmatlar", "Isitish va sovitish xizmatlari", "Услуги по отоплению и охлаждению");
        create("Xizmatlar", "Parda va jalyuzi xizmatlari", "Услуги штор и жалюзи");
        create("Xizmatlar", "Kunlik ishchilar xizmati", "Служба поденщиков");

        create("Elektr jihozlar", "Telefonlar", "Телефоны");
        create("Elektr jihozlar", "Video Texnika", "Видео Техника");
        create("Elektr jihozlar", "Audiotexnika", "Аудиотехника");
        create("Elektr jihozlar", "Uy uchun texnika", "Техника для дома");
        create("Elektr jihozlar", "Kompyuterlar", "Компьютеры");
        create("Elektr jihozlar", "Foto va Video texnika", "Фото и видео техника");

        create("Ko'chmas mulk", "Kvartiralar", "Квартиры");
        create("Ko'chmas mulk", "Garajlar", "Гаражи");
        create("Ko'chmas mulk", "Xususiy uylar", "Частные дома");
        create("Ko'chmas mulk", "Yer uchastkalar", "Земельные участки");
        create("Ko'chmas mulk", "Tijorat binolar", "Коммерческие здания");

        create("Moda va still", "Erkaklar uchun kiyimlar", "Одежда для мужчин");
        create("Moda va still", "Qo'l soatlar", "Часы");
        create("Moda va still", "Ayollar uchun kiyimlar", "Одежда для женщин");
        create("Moda va still", "Oyog' buyumlar", "Обувь");
        create("Moda va still", "To'y va ma'rosimlar uchun liboslar", "Платья для свадеб и церемоний");
        create("Moda va still", "Parfyumerlar", "Парфюмеры");
        create("Moda va still", "Aksesuarlar", "Аксессуары");

        create("Bolalar dunyosi", "Bolalar kiyimlari", "Детская одежда");
        create("Bolalar dunyosi", "Bolalar oyoq kiyimi", "Детские туфли");
        create("Bolalar dunyosi", "Bolalar mebeli", "Детская мебель");
        create("Bolalar dunyosi", "O'yinchoqlar", "Игрушки");
        create("Bolalar dunyosi", "Kolyoskalar", "Коляски");
        create("Bolalar dunyosi", "Maktab o'quvchilari uchun maxsulotlar", "Товары для школьников");
    }
}
