# Imports
from subprocess import run, PIPE, DEVNULL
from tkinter import *
from tkinter.font import *
# noinspection PyUnresolvedReferences
from threading import Thread
# noinspection PyUnresolvedReferences
from tkinter.ttk import Progressbar


# Functions
def cancel_install():
    global window
    cancel_win = Toplevel()
    cancel_win.title("Cancel")
    cancel_win.config(width=300, height=150)
    cl = Label(cancel_win, text="Cancel the installation?", padx=7, pady=7)
    cl.pack()
    cf = Frame(cancel_win)
    cf.pack()
    cb = Button(cf, text="Yes", command=window.destroy)
    cb.pack(side=LEFT, anchor=W)
    ccb = Button(cf, text="No", command=cancel_win.destroy)
    ccb.pack(side=LEFT, anchor=W)


# noinspection PyShadowingNames
def page1():
    global p1
    global bold
    global window
    global imglist
    global p2
    p2.pack_forget()
    # Page 1 (Start)
    p1 = Frame()
    p1.pack(fill=BOTH)

    # Installer image
    installer_img = Canvas(p1, height=512, width=290)
    installer_img2 = PhotoImage(file="installer.png")
    imglist.append(installer_img2)
    # noinspection PyUnusedLocal
    image = installer_img.create_image(0, 0, anchor='nw', image=installer_img2)
    installer_img.pack(side=LEFT)

    # Text
    tf = Frame(p1)
    tf.pack(anchor=NW)
    tl = Label(tf, text="Welcome to the Package Installation Wizard", anchor=W, padx=7, pady=7, width=120, font=bold)
    tl.pack(anchor=NW)
    tl2 = Label(tf, text="This wizard will guide you through the package installation process.", anchor=W, padx=7,
                pady=7,
                width=120, wraplength=380, justify=LEFT)
    tl2.pack(anchor=NW)
    tl3 = Label(tf, text='Click "Next" to get started.', anchor=W, padx=7, pady=7, width=120)
    tl3.pack(anchor=NW)
    # Buttons
    bf = Frame(p1)
    cb = Button(bf, text="Cancel", command=cancel_install)
    nb = Button(bf, text="Next >", command=page2)
    bb = Button(bf, text="< Back", state=DISABLED)
    bf.pack(side=RIGHT, anchor=SE)
    cb.pack(side=RIGHT, anchor=SE, padx=5, pady=5)
    nb.pack(side=RIGHT, anchor=SE, padx=5, pady=5)
    bb.pack(side=RIGHT, anchor=SE, padx=5, pady=5)


def p_install():
    global nb
    global selected
    selected = "install"
    nb.config(state=NORMAL)


def p_remove():
    global selected
    global nb
    nb.config(state=NORMAL)
    selected = "remove"


def p_search():
    global selected
    global nb
    nb.config(state=NORMAL)
    selected = "search"


# noinspection PyShadowingNames
def page2():
    global nb
    global window
    global bold
    global p2
    global imglist
    # Remove page 1 + 3
    global p1
    global p3
    p1.pack_forget()
    p3.pack_forget()
    # Main frame
    p2 = Frame(window)
    p2.pack(fill=BOTH, expand=True)
    # Header
    top = Frame(p2, borderwidth=2, relief=SOLID)
    top.pack(anchor=NW, fill=X)
    # Image
    installer_img = Canvas(top, height=64, width=64)
    installer_img2 = PhotoImage(file="installer_2.png")
    imglist.append(installer_img2)
    # noinspection PyUnusedLocal
    image = installer_img.create_image(0, 0, anchor='nw', image=installer_img2)
    installer_img.pack(side=RIGHT, padx=3, pady=3)

    # Label
    il = Label(top, text="Installation Wizard", font=bold, padx=7, anchor=W, width=120)
    il.pack(anchor=W, side=LEFT)

    # Main area
    main = Frame(p2)
    main.pack(side=TOP, fill=BOTH, expand=True, anchor=NW)

    sl = Label(main, text="Select the action you want to perform:", font=bold, padx=7, pady=7, anchor=W, width=120)
    sl.pack(anchor=NW)
    # Radiobuttons
    choice = StringVar()
    install = Radiobutton(main, text="Install package(s)", value="Install", variable=choice, command=p_install)
    install.pack(anchor=NW, padx=7, pady=7)
    remove = Radiobutton(main, text="Remove package(s)", value="remove", variable=choice, command=p_remove)
    remove.pack(anchor=NW, padx=7, pady=7)
    search = Radiobutton(main, text="Search for package(s)", value="search", variable=choice, command=p_search)
    search.pack(anchor=NW, padx=7, pady=7)

    # Buttons
    bf = Frame(p2)
    cb = Button(bf, text="Cancel", command=cancel_install)
    nb = Button(bf, text="Next >", command=page3, state=DISABLED)
    bb = Button(bf, text="< Back", command=page1)
    bf.pack(side=RIGHT, anchor=SE)
    cb.pack(side=RIGHT, anchor=SE, padx=5, pady=5)
    nb.pack(side=RIGHT, anchor=SE, padx=5, pady=5)
    bb.pack(side=RIGHT, anchor=SE, padx=5, pady=5)


# noinspection PyUnresolvedReferences
def search():
    global search_entry
    global results
    global window
    global lb_search
    lb_search.delete(0, END)
    if not search_entry.get():
        lb_search.insert(END, "Empty query.")
        return
    result = run(["apt-cache", "search", search_entry.get()], stdout=PIPE)
    resultlst = result.stdout.decode('utf-8').splitlines()
    if not resultlst:
        lb_search.insert(END, "No results found.")
    for i in resultlst:
        lb_search.insert(END, i)


# noinspection PyUnresolvedReferences
def install_add():
    global lb_install
    global install_entry
    global el
    el.pack_forget()
    if not run(["apt-cache", "show", install_entry.get()], stdout=DEVNULL, stderr=DEVNULL).returncode.__bool__():
        lb_install.insert(END, install_entry.get())
    else:
        el.pack(anchor=W, side=LEFT)
    if lb_install.get(0, END):
        nb.config(state=NORMAL)
    else:
        nb.config(state=DISABLED)


# noinspection PyUnresolvedReferences
def get_deps(package):
    global lb_install
    global install_list
    depends = run(
        ["sh", "-c", f'echo $(apt-rdepends {package} | grep -v "^ " | grep -v "^libc-dev$")'],
        stdout=PIPE, stderr=DEVNULL).stdout.decode(
        'utf-8').split()
    for i in depends[:]:
        if i.endswith(":any"):
            i2 = i.removesuffix(":any")
        else:
            i2 = i

        virtpackage = run(["sh", "-c", f"grep-available -F Provides -s Package {i2} | sed 's/Package: //'"],
                          stdout=PIPE, stderr=DEVNULL).stdout.decode().splitlines()
        if virtpackage:
            virtpackage = virtpackage[0]
            i2 = virtpackage

        if not run(["dpkg", "-s", i2], stdout=DEVNULL, stderr=DEVNULL).returncode.__bool__():
            depends.remove(i)

    conflicts = run(["sh", "-c",
                     f"grep-status -P {package} --exact-match -s Conflicts | sed -e 's/Conflicts: //' | tr ',"
                     f"' ' ' | xargs"], stdout=PIPE).stdout.decode('utf-8').split()
    recommends = run(["sh", "-c",
                      f"grep-status -P {package} --exact-match -s Recommends | sed -e 's/Recommends: //' -e 's/|//' "
                      f"-e 's/([^()]*)//g' | tr ',' ' ' | xargs"], stdout=PIPE).stdout.decode('utf-8').split()
    suggests = run(
        ["sh", "-c", f"grep-status -P {package} --exact-match -s Suggests | sed -e 's/Suggests: //' -e 's/,//'"],
        stdout=PIPE).stdout.decode('itf-8').split()
    breaks = run(["sh", "-c",
                  f"grep-status -P {package} --exact-match -s Breaks | sed -e 's/Breaks: //' -e 's/|//' -e 's/([^("
                  f")]*)//g'"
                  f"| tr ',' ' ' | xargs"],
                 stdout=PIPE).stdout.decode('utf-8').split()
    return {deps: depends, recom: recommends, suggest: suggests, breaks: breaks, conflict: conflicts}


def remove_active():
    global lb_install
    lb_install.delete(ACTIVE)
    if lb_install.get(0, END):
        nb.config(state=NORMAL)
    else:
        nb.config(state=DISABLED)


# noinspection PyUnusedLocal
def page3():
    global nb
    global window
    global bold
    global imglist
    global p3
    global selected
    global results
    global search_entry
    global lb_install
    global lb_search
    global install_entry
    global el
    global inf

    # Remove page 2 & 3
    global p2
    global p4
    p2.pack_forget()
    p4.pack_forget()

    p3 = Frame(window)
    p3.pack(fill=BOTH, expand=True)
    top = Frame(p3, borderwidth=2, relief=SOLID)
    top.pack(anchor=NW, fill=X)
    # Image
    installer_img = Canvas(top, height=64, width=64)
    installer_img2 = PhotoImage(file="installer_2.png")
    imglist.append(installer_img2)
    image = installer_img.create_image(0, 0, anchor='nw', image=installer_img2)
    installer_img.pack(side=RIGHT, padx=3, pady=3)

    # Label
    il = Label(top, text="Installation Wizard", font=bold, padx=7, anchor=W, width=120)
    il.pack(anchor=W, side=LEFT)

    # Main content
    main = Frame(p3)
    main.pack(side=TOP, fill=BOTH, expand=True, anchor=SW)
    if selected == "install":

        inf = Frame(main)
        inf.pack(anchor=NW, fill=X, expand=True)
        il = Label(inf, text="Package: ", padx=7, pady=7)
        il.pack(side=LEFT, anchor=W)

        install_entry = Entry(inf)
        install_entry.pack(side=LEFT, anchor=W, padx=7, pady=7)
        ib = Button(inf, text="Add", command=install_add)
        ib.pack(side=LEFT, anchor=W, padx=7, pady=7)
        db = Button(inf, text="Remove selected", command=remove_active)
        db.pack(side=LEFT, anchor=W, padx=7, pady=7)

        el = Label(inf, text="ERROR: Package not found!", fg="red", padx=7, pady=7)

        textframe = Frame(main)
        textframe.pack(fill=BOTH, expand=True, anchor=NW, padx=7)

        lb_install = Listbox(textframe)

        lb_install.config(height=15)
        lb_install.pack(side=LEFT, fill=BOTH, expand=1)
        scrollbary = Scrollbar(textframe, orient=VERTICAL, command=lb_install.yview)
        scrollbary.pack(side=RIGHT, fill=Y)

        lb_install["yscrollcommand"] = scrollbary.set
        print("DISABLING")

        # install command: sh -c "apt-get install -o APT::Status-Fd=2 -y gtk-3-examples | grep -e pmstatus -e dlstatus"

    elif selected == "search":
        sf = Frame(main)
        sf.pack(anchor=NW, fill=X, expand=True)
        sl = Label(sf, text="Query: ", padx=7, pady=7)
        sl.pack(side=LEFT, anchor=W)

        search_entry = Entry(sf)
        search_entry.pack(side=LEFT, anchor=W, padx=7, pady=7)
        sb = Button(sf, text="Search", command=search)
        sb.pack(side=LEFT, anchor=W, padx=7, pady=7)

        textframe = Frame(main)
        textframe.pack(fill=BOTH, expand=True, anchor=NW, padx=7)

        # noinspection PyTypeChecker
        languages_var = StringVar(value=results)
        lb_search = Listbox(textframe, listvariable=languages_var)

        lb_search.config(height=15)
        scrollbarx = Scrollbar(textframe, orient=HORIZONTAL, command=lb_search.xview)
        scrollbarx.pack(side=BOTTOM, fill=X)
        lb_search.pack(side=LEFT, fill=BOTH, expand=1)
        scrollbary = Scrollbar(textframe, orient=VERTICAL, command=lb_search.yview)
        scrollbary.pack(side=RIGHT, fill=Y)

        lb_search["yscrollcommand"] = scrollbary.set
        lb_search["xscrollcommand"] = scrollbarx.set
        nb.config(state=ACTIVE)
    elif selected == "remove":
        pass
    # Buttons
    bf = Frame(p3)
    cb = Button(bf, text="Cancel", command=cancel_install)
    nb = Button(bf, text="Next >", command=page4, state=DISABLED)
    bb = Button(bf, text="< Back", command=page2)
    bf.pack(side=RIGHT, anchor=SE, fill=X, expand=True)
    cb.pack(side=RIGHT, anchor=SE, padx=5, pady=5)
    nb.pack(side=RIGHT, anchor=SE, padx=5, pady=5)
    bb.pack(side=RIGHT, anchor=SE, padx=5, pady=5)


def page4():
    global selected
    if selected == "install":
        install_options()
    elif selected == "remove":
        pass
    elif selected == "search":
        pass


def install_options():
    global p4
    global imglist
    global lb_install
    global window
    # Remove page 3
    global p3
    p3.pack_forget()

    p4 = Frame(window)
    p4.pack(fill=BOTH, expand=True)
    top = Frame(p4, borderwidth=2, relief=SOLID)
    top.pack(anchor=NW, fill=X)
    # Image
    installer_img = Canvas(top, height=64, width=64)
    installer_img2 = PhotoImage(file="installer_2.png")
    imglist.append(installer_img2)
    # noinspection PyUnusedLocal
    image = installer_img.create_image(0, 0, anchor='nw', image=installer_img2)
    installer_img.pack(side=RIGHT, padx=3, pady=3)

    # Label
    il = Label(top, text="Installation Wizard", font=bold, padx=7, anchor=W, width=120)
    il.pack(anchor=W, side=LEFT)

    # Main content
    main = Frame(p4)
    main.pack(side=TOP, fill=BOTH, expand=True, anchor=SW)

    # Buttons
    bf = Frame(p4)
    cb = Button(bf, text="Cancel", command=cancel_install)
    # noinspection PyShadowingNames
    nb = Button(bf, text="Next >")
    bb = Button(bf, text="< Back", command=page3)
    bf.pack(side=RIGHT, anchor=SE, fill=X, expand=True)
    cb.pack(side=RIGHT, anchor=SE, padx=5, pady=5)
    nb.pack(side=RIGHT, anchor=SE, padx=5, pady=5)
    bb.pack(side=RIGHT, anchor=SE, padx=5, pady=5)


# Window parameters
window = Tk()
window.geometry("700x512")
window.title("Package Installation Wizard")
window.resizable(height=False, width=False)

# Close button
window.protocol('WM_DELETE_WINDOW', cancel_install)

# Variables
bold = Font(weight="bold")
# Pages
p1 = Frame()
p2 = Frame()
p3 = Frame()
p4 = Frame()

imglist = []
selected = ""
nb = Button()
results = []
search_entry = None
lb_search = None
lb_install = Listbox()
install_entry = None
el = None
install_list = []
inf = Frame()
pb = None

page1()  # Main code

window.mainloop()
