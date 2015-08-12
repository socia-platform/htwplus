package models;

import models.base.BaseModel;
import play.data.validation.Constraints;
import play.db.jpa.JPA;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Folder extends BaseModel {

    @Constraints.Required
    public String name;

    @Constraints.Required
    @ManyToOne
    public Account owner;

    @ManyToOne
    public Folder parent;

    @ManyToOne
    public Group group;

    @ManyToOne
    public Account account;

    @OneToMany(mappedBy = "folder")
    @OrderBy("createdAt DESC")
    public List<Media> files;

    @OneToMany(mappedBy = "parent")
    public List<Folder> folders;

    @Transient
    List<Folder> folderList = new ArrayList<>();

    public static Folder findById(long id) {
        return JPA.em().find(Folder.class, id);
    }

    @Override
    public void create() {
        JPA.em().persist(this);
    }

    @Override
    public void update() {
        JPA.em().merge(this);
    }

    @Override
    public void delete() {
        JPA.em().remove(this);
    }

    public Folder() {}

    public Folder(String name, Account owner, Folder parent, Group group, Account account) {
        this.name = name;
        this.owner = owner;
        this.parent = parent;
        this.group = group;
        this.account = account;
        create();
    }

    public Folder findRoot(Folder folder) {
        if(folder.parent == null) return folder;
        return findRoot(folder.parent);
    }

    /**
     * Find all ancestors without root folder.
     * used for breadcrumb navigation
     * @param folder
     * @return
     */
    public List<Folder> findAncestors(Folder folder) {
        if(folder.parent != null) {
            folderList.add(folder);
            findAncestors(folder.parent);
        }
        return folderList;
    }

}
