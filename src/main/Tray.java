package main;

import database.ProjectDAO;
import models.Project;
import utils.OSValidator;
import utils.Terminal;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;

public class Tray {
    private TrayIcon trayIcon;
    private SystemTray tray;
    private ArrayList<Project> projects;
    private ProjectDAO projectDAO;

    public Tray() {
        projectDAO = new ProjectDAO();
        loadProjects();

        tray = SystemTray.getSystemTray();
        renderTrayIcon();
    }

    public void show() {
        if (!SystemTray.isSupported()) {
            JOptionPane.showMessageDialog(null, "Recurso ainda nao esta disponivel pra o seu sistema");
            return;
        }

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println("Erro, TrayIcon nao sera adicionado.");
            e.printStackTrace();
        }
    }

    private ArrayList<Project> loadProjects() {
        projects = projectDAO.findAll();
        return projects;
    }

    private Image getIcon(String path) {
        try {
            return ImageIO.read(getClass().getClassLoader().getResourceAsStream(path));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private TrayIcon renderTrayIcon() {
        trayIcon = new TrayIcon(getIcon("images/icon.png"), "Code Tray", renderPopupMenu());
        trayIcon.setImageAutoSize(true);
        return trayIcon;
    }

    private PopupMenu renderPopupMenu() {
        PopupMenu popupMenu = new PopupMenu("Menu");

        MenuItem addProjectItem = new MenuItem("Adicionar projeto");
        addProjectItem.addActionListener(onAddNewProject());
        MenuItem exitItem = new MenuItem("Sair");
        exitItem.addActionListener(onExit());

        popupMenu.add(addProjectItem);
        if (projects.size() > 0) {
            popupMenu.addSeparator();
            renderProjects(popupMenu);
        }
        popupMenu.addSeparator();
        popupMenu.add(exitItem);

        return popupMenu;
    }

    private void renderProjects (PopupMenu popupMenu) {
        for (Project project: projects) {
            PopupMenu projectPopup = new PopupMenu(project.getName());

            MenuItem itemOpenFolder = new MenuItem("Abrir pasta");
            itemOpenFolder.addActionListener(onOpenFolder(project));
            MenuItem itemOpenInVSCode = new MenuItem("Abrir com VSCode");
            itemOpenInVSCode.addActionListener(onOpenWithVSCode(project));
            MenuItem itemRemove = new MenuItem("Remover");
            itemRemove.addActionListener(onRemoveItem(project));

            projectPopup.add(itemOpenFolder);
            projectPopup.add(itemOpenInVSCode);
            projectPopup.add(itemRemove);

            popupMenu.add(projectPopup);
        }
    }
    
    private void updateTray() {
        if (OSValidator.isWindows()) {
            tray.getTrayIcons()[0].setPopupMenu(renderPopupMenu());
            return;
        }

        if (OSValidator.isMac()) {
            tray.remove(trayIcon);
            renderTrayIcon();
            show();
            return;
        }
    }
    
    private ActionListener onOpenFolder (Project project) {
        return e -> {
            try {
                Terminal.openFolder(project.getPath());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };
    }

    private ActionListener onOpenWithVSCode (Project project) {
        return e -> {
            try {
                Terminal.openVSCode(project.getPath());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };
    }

    private ActionListener onExit () {
        return e -> System.exit(0);
    }

    private ActionListener onAddNewProject () {
        return e -> {
            AddProjectView addProjectView = new AddProjectView((path, name) -> {
                Project project = new Project(45, path, name);
                projects.add(project);
                updateTray();
                projectDAO.store(project);
            });
            addProjectView.show();
        };
    }

    private ActionListener onRemoveItem (Project project) {
        return e -> {
            projects.remove(project);
            updateTray();
            projectDAO.delete(project.getId());
        };
    }
}
