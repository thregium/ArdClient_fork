class Script:
    def run(self):
        self.window = self.v.pbotutils().PBotWindow(self.v.ui, "Butcher", 110, 110, self.v.scriptid)
        self.start = self.window.addButton("skin", "Start", 100, 5, 85)
        self.a = "4"

    def skin(self):
        self.v.pbotutils().sysMsg(self.v.ui, self.a)
