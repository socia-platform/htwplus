package models;

import models.base.BaseModel;
import play.data.validation.Constraints;

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

    public Folder() {}

    public Folder(String name, Account owner, Folder parent, Group group, Account account) {
        this.name = name;
        this.owner = owner;
        this.parent = parent;
        this.group = group;
        this.account = account;
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
