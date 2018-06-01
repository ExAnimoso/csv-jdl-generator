package com.sunrise.jdl.generator;

import com.sunrise.jdl.generator.entities.Entity;
import com.sunrise.jdl.generator.entities.Field;
import com.sunrise.jdl.generator.entities.Relation;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Считывает Entity, корректириреут их поля, создает их структуру,
 * записывает Entity и их структуру в файл
 */
public class EntitiesService {

    /**
     * Константые поля содержат номера ячеек в электронной таблице,
     * из которых берутся соотвествующие значения.
     */
    public static final int CLASSNAME = 1;
    public static final int FIELDNAME = 2;
    public static final int FIELDTYPE = 5;
    public static final int FIELDSIZE = 6;
    public static final String LIST_TYPE = "Список";

    private final Set<String> convertableToJdlTypes = new HashSet<>();

    public EntitiesService(){
        convertableToJdlTypes.add("Строка");
        convertableToJdlTypes.add("Число");
        convertableToJdlTypes.add("Дата/время");
    }

    public List<Entity> readAll(List<InputStream> resources) {
        ArrayList<Entity> entities = new ArrayList<Entity>();
        for (InputStream st : resources) {
            entities.addAll(readDataFromCSV(st));
        }
        return entities;
    }

    /**
     * Метод читает сущности из передаваемого .csv файла
     *
     * @param stream Поток с данными о сущностях
     * @return entities Список сущностей сформированных на основе потока данных
     */
    private java.util.Collection<Entity> readDataFromCSV(InputStream stream) {
        Map<String, Entity> toReturn = new LinkedHashMap<String, Entity>();
        try {
            Reader in = new InputStreamReader(stream);
            Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
            String className = "";
            for (CSVRecord record : records) {
                String possibleClassName = record.get(CLASSNAME);
                String fieldName = record.get(FIELDNAME);
                String fieldType = record.get(FIELDTYPE);
                String fieldLength = record.get(FIELDSIZE);

                if (!possibleClassName.equals("") && !possibleClassName.contains("П") && !possibleClassName.isEmpty()) {
                    className = possibleClassName;
                    Field field = new Field(convertFieldType(fieldType), fieldName, fieldLength, isFieldOfJdlType(fieldType));
                    ArrayList<Field> arrayList = new ArrayList<Field>();
                    arrayList.add(field);
                    Entity entity = new Entity(className, arrayList);
                    toReturn.put(className, entity);
                } else if (possibleClassName.equals("") && toReturn.size() > 0) {
                    toReturn.get(className).getFields().add(new Field(convertFieldType(fieldType), fieldName, fieldLength, isFieldOfJdlType(fieldType)));
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Can't find file");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IO exception");
            e.printStackTrace();
        }
        return toReturn.values();
    }

    /**
     * Конвертировать тип из формата описания в JDL
     * @param source Исходные данные для конвертации
     * @return В случае если нелзья сконвертировать, возвращает исходыне данные.
     */
    public String convertFieldType(String source){
        if(!isFieldOfJdlType(source)){
            return source;
        }
        if (source.contains("Строка")) {
            return JDLFieldsType.String.toString();
        }
        if (source.contains("Дата")) {
            return JDLFieldsType.Instant.toString();

        }
        if (source.contains("Число")) {
            return JDLFieldsType.Long.toString();
        }
        throw new RuntimeException("Неудалось распарсить исходыне данные в JDL тип");
    }

    /**
     * Для каждой entity из entities вызывается метод createStructure(Entity entity)
     *
     * @param entities
     * @return total number of created structure
     */
    public int createStructure(List<Entity> entities) {
        int totalCreatedStructure = 0;
        for (Entity entity : entities) {
            totalCreatedStructure += this.createStructure(entity);
        }
        return totalCreatedStructure;
    }

    /**
     * Если сущность содержит в fields Список, метод создает объект Relation и
     * добавляет его в поле relations у Entity.
     * Метод возращает количество считанных структур.
     *
     * @param entity
     * @return number of created structure
     */
    public int createStructure(Entity entity) {
        int count = 0;
        ArrayList<Field> fields = entity.getFields();
        for (Field field : fields) {
            if (!field.isJdlType() && field.getFieldType().contains(LIST_TYPE)) {
                count++;
                Relation relation = new Relation(entity, field, Relation.RelationType.OneToMany);
                entity.getRelations().add(relation);
            } else if (!field.isJdlType()) {
                Relation relation = new Relation(entity, field, Relation.RelationType.OneToOne);
                entity.getRelations().add(relation);
                count++;
            }
        }
        return count;
    }

    /**
     * Перегруженный вариант writeEntityToFile(Entity entity, BufferedWriter writer).
     * Для каждой entity из enities вызывается метод writeEntityToFile(Entity entity, BufferedWriter writer)
     *
     * @param entities
     * @param writer
     * @return
     */
    public void writeEntityToFile(List<Entity> entities, BufferedWriter writer) throws IOException {
        for (Entity entity : entities) {
            writeEntityToFile(entity, writer);
        }
    }

    /**
     * Метод пишет сущности с полями и структурами в файл в формате JDL
     *
     * @param entity
     * @param writer
     * @throws IOException
     */
    public void writeEntityToFile(Entity entity, BufferedWriter writer) throws IOException {
        writer.write(entity.toString() + "\n");
        ArrayList<Relation> relations = entity.getRelations();
        if (relations.size() > 0) {
            for (Relation relation : relations) {
                writer.write(relation.toString() + "\n");
            }
        }
    }

    /**
     * Возвращает истину если @fieldType может быть преобразован к типу jdl
     *
     * @param fieldType Тип который надо проверить
     * @return Истина если можно привести к формату jdl иначе ложь
     */
    private boolean isFieldOfJdlType(String fieldType) {
        return convertableToJdlTypes.contains(fieldType);
    }


    /**
     * Перечисление содержит поддерживаемые в JDL типы полей.
     */
    public enum JDLFieldsType {

        String("String"),

        Integer("Integer"),

        Long("Long"),

        Float("Float"),

        Double("Double"),

        BigDecimal("BigDecimal"),

        LocalDate("LocalDate"),

        Instant("Instant"),

        ZonedDateTime("ZonedDateTime"),

        Boolean("Boolean"),

        Enumeration("Enumeration"),

        Blob("Blob");

        /**
         * Строковое представление типа в формате JDL
         */
        private String type;

        /**
         * Конструктор
         *
         * @param type
         */
        JDLFieldsType(java.lang.String type) {
            this.type = type;
        }


        @Override
        public java.lang.String toString() {
            return type;
        }
    }
}