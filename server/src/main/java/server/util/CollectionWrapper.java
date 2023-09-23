package server.util;


import model.SpaceMarine;
import server.exceptions.AccessException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class CollectionWrapper {
    private final DatabaseProcessor dbProcessor;
    private final String type;
    private final Date date;
    private CopyOnWriteArrayList<SpaceMarine> data;
    private int count;

    public CollectionWrapper(DatabaseProcessor dbProcessor) throws Exception {
        this.dbProcessor = dbProcessor;
        data = dbProcessor.getCollection();

        if (!isEmptyData()) {
            sortData();
        }
        type = SpaceMarine.class.getName();
        date = new Date();
    }

    public synchronized void setConnection(String credentialsName) throws SQLException, IOException {
        dbProcessor.setConnection(credentialsName);
    }

    public synchronized Connection getConnection() {
        return dbProcessor.getConnection();
    }

    public synchronized void checkDatabase() {
        dbProcessor.checkDatabase();
    }

    public synchronized CopyOnWriteArrayList<SpaceMarine> getData() {
        return data;
    }

    public synchronized void setData(CopyOnWriteArrayList<SpaceMarine> data) {
        this.data = data;
    }

    public synchronized Date getDate() {
        return date;
    }

    public synchronized int getCount() {
        count = data.size();
        return count;
    }

    public synchronized boolean isEmptyData() {
        if (data.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    public synchronized String getType() {
        return type;
    }

    public synchronized void clearData(String username) throws SQLException {
        if (!isEmptyData()) {
            dbProcessor.clear(username);
            if (data.isEmpty()) dbProcessor.restartSequence();
            data.removeIf(sp -> sp.getUsername().equals(username));
        }
    }

    public synchronized void removeFirstItem(String username) throws SQLException, AccessException {
        if (!isEmptyData()) {
            if (!data.get(0).getUsername().equals(username))
                throw new AccessException("Вы можете редактировать только собственные записи!");
            data.remove(0);
            dbProcessor.remove(data.get(0).getId());
            if (data.isEmpty()) dbProcessor.restartSequence();
            System.out.println("Объект удалён");
        } else {
            System.out.println("Коллекция пуста - нечего удалять");
        }
    }

    public synchronized void removeByIdItem(String username, int id) throws SQLException, AccessException {
        if (!isEmptyData()) {
            for (SpaceMarine sp : data) {
                if (sp.getId() == id) {
                    boolean access = sp.getUsername().equals(username);
                    if (!access)
                        throw new AccessException("Вы можете редактировать только собственные записи!");

                    dbProcessor.remove(id);
                    data.remove(sp);
                    System.out.println("Объект удалён");
                }
            }
        } else {
            System.out.println("Коллекция пуста - нечего удалять");
        }
        if (data.isEmpty()) dbProcessor.restartSequence();
    }

    public synchronized void addItem(String username, SpaceMarine sp) throws SQLException {
        int spId = dbProcessor.insertMarine(username, sp);
        sp.setId(spId);
        sp.setUsername(username);

        data.add(sp);
    }

    public synchronized void updateItem(String username, int id, SpaceMarine newSp)
            throws SQLException, AccessException {

        if (!isEmptyData()) {
            for (SpaceMarine sp : data) {
                if (sp.getId() == id) {
                    if (!sp.getUsername().equals(username))
                        throw new AccessException("Вы можете редактировать только собственные записи!");

                    sp.setName(newSp.getName());
                    sp.setCoordinatesX(newSp.getCoordinates().getX());
                    sp.setCoordinatesY(newSp.getCoordinates().getY());
                    sp.setHealth(newSp.getHealth());
                    sp.setHeartCount(newSp.getHeartCount());
                    sp.setAchievements(newSp.getAchievements());
                    sp.setCategory(sp.getCategory());
                    sp.setChapterName(sp.getChapter().getName());
                    sp.setChapterMarinesCount(newSp.getChapter().getMarinesCount());
                    dbProcessor.update(sp);
                    System.out.println("Объект обновлён");
                }
            }
        } else {
            System.out.println("Коллекция пуста - нечего обновлять");
        }
    }

    public synchronized void sortData() {
        Collections.sort(data);
    }
}

