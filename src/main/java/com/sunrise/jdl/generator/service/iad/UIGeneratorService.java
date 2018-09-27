package com.sunrise.jdl.generator.service.iad;

import com.sunrise.jdl.generator.actions.Action;
import com.sunrise.jdl.generator.entities.Field;
import com.sunrise.jdl.generator.forJson.BaseField;
import com.sunrise.jdl.generator.forJson.ProjectionInfo;
import com.sunrise.jdl.generator.forJson.RegistryItem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис генерации данных визуального интерфейса
 */
public class UIGeneratorService {

    private static final String PRESENTATION_CODE_TEMPLATE = "%sPresentation";


    /**
     * Преобразовать данные о сщуности в информацию о проекции
     * @param entityName Название сущности
     * @param fields поля сущности
     * @param actions доступные действия для проекции
     * @param presentationCode Код представления
     * @return Информация о проекции
     */
    public ProjectionInfo toProjectionInfo(String entityName, Set<Field> fields, Collection<Action> actions, String presentationCode) {
        ProjectionInfo projectionInfo = new ProjectionInfo();
        projectionInfo.setCode(entityName);
        projectionInfo.setName(entityName);
        projectionInfo.setParentCode(presentationCode);
        projectionInfo.setListFields(fields.stream().map(f -> new BaseField(f)).collect(Collectors.toList()));
        projectionInfo.setActions(new ArrayList<>(actions));
        return projectionInfo;
    }

    /**
     * Создать представление на основе информации о названии сущности и коде реестра
     *
     * @param entityName   Название сущности
     * @param registryCode код  реестра интерфейсов
     * @return Базовая инфомрация о представлении созданная на основе @entityName and @registryCode
     */
    public RegistryItem createPresenationFor(String entityName, String registryCode) {
        RegistryItem item = new RegistryItem();
        item.setParentCode(registryCode);
        item.setCode(getPresentationName(entityName));
        item.setName(item.getCode());
        return item;
    }

    /**
     * Очистить целевую диркторию  генерации файлов описания интерфейса
     *
     * @param targetDirectory Целевая директория
     * @throws IOException Возникает в случае если невозможно очистить директорию
     */
    public void cleanupTargetDirecotry(Path targetDirectory) throws IOException {
        if (Files.exists(targetDirectory)) {
            try {
                Files.walk(targetDirectory)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                throw new IOException("Ошибка при очистке целевой директории: " + Arrays.toString(e.getStackTrace()));
            }
        }
    }


    private String getPresentationName(String entityName) {
        return String.format(PRESENTATION_CODE_TEMPLATE, entityName);
    }


}