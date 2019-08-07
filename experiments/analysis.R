library(lattice)
library(plotrix)
library(scales)

mergedata <- read.csv("merged.csv")
avg = aggregate(list(OurAvg = mergedata$Ours, TheirAvg = mergedata$Theirs), list(Swaps = as.numeric(factor(mergedata$Swaps)), Group=mergedata$Group), mean)
avgdata <- merge(mergedata,avg,by=c("Swaps","Group"))
avgdf <- avgdata[order(avgdata$Group, avgdata$Swaps),]

xyplot(Ours+Theirs+OurAvg+TheirAvg~Swaps | Group, 
       data=avgdf, 
       type=c("p","p","smooth","smooth","g"),
       layout=c(3,1),
       panel = function(...) {
         grpname <- dimnames(trellis.last.object())[[1]][packet.number()]
         if(grpname == "6 wires") {
           panel.rect(14.5, -10,55, 10, col = "Lightgray", border=NA)
         } else if(grpname == "5 wires") {
           panel.rect(8.5, -10, 55, 10, col = "Lightgray", border=NA)
         } else if(grpname == "7 wires") {
           panel.rect(21.5, -10, 55, 10, col = "Lightgray", border=NA)
         }
         panel.xyplot(...)
       },
       scales=list(alternating=FALSE,
                   y = list(log = 10),
                   x = list(relation="free")), 
       par.settings = list(superpose.symbol = list(pch = c(20,17,3), 
                                                   cex = c(0.5,0.5)),
                           superpose.line = list(lwd=2)),
       col = c(alpha("#1f78b4",0.4), alpha("#fb9a99",0.4), "#1f78b4", "#fb9a99"),
       ylab = "Time in seconds",
       xlab= "|L|",
       ylim = c(0.00004,7200),
       distribute.type=TRUE)
