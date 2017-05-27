import java.lang.*;
import java.awt.*;
import javax.swing.*;

public class PagingManagement extends JFrame
{
    public static void main(String[] args)
    {
        userInterface ui = new userInterface("Memory Paging Management");//创建图形界面
        ui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ui.show();
        ui.memory.lru();
    }
}

class userInterface extends JFrame//用户界面类
{
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;

    public Container mainPanel;//主面板
    public infoPanel info;//输出信息面板
    public memoryPanel memory;//内存显示面板


    public userInterface(String str)
    {
        setTitle("Memory Paging Management");
        setSize(WIDTH,HEIGHT);
        mainPanel=getContentPane();//画出界面
        mainPanel.setLayout(new GridLayout(1,2));

        info=new infoPanel();
        memory=new memoryPanel(4,32,320,info);

        mainPanel.add(info);
        mainPanel.add(memory);

    }
}

class infoPanel extends JPanel//信息面板
{
    public JTextArea missCountDisplay;//缺页数
    public JTextArea missRateDisplay;//缺页率
    public JTextArea cacheInfo;//指令信息
    public JScrollPane cacheInfoDisplay;//指令信息输出

    public infoPanel()
    {
        setLayout(new GridLayout(3,1));

        missCountDisplay=new JTextArea("Page Missing Count:");
        missRateDisplay=new JTextArea("Page Missing Rate:");
        cacheInfo=new JTextArea();

        cacheInfo.setLineWrap(true);
        cacheInfoDisplay=new JScrollPane(cacheInfo);
        cacheInfoDisplay.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        Font f=new Font("Helvetica",Font.PLAIN,15);
        missCountDisplay.setFont(f);
        missRateDisplay.setFont(f);
        cacheInfo.setFont(f);

        add(missCountDisplay);
        add(missRateDisplay);
        add(cacheInfoDisplay);
    }

}

class page
{
    public int pageID;//页号
    public page prev;//前一页
    public page next;//后一页
    public page(int id,page prev,page next)
    {
        this.pageID=id;
        this.prev=prev;
        this.next=next;
    }
}

class memoryPanel extends JPanel//内存面板
{
    private final int capacity;//内存容量
    private int pageNum;//页面数
    private int instructionNum;//指令数
    private int miss;//缺页数
    private int size;//现有容量

    private page head;//链表头
    private page tail;//链表尾

    private page []memoryBlocks;//物理内存
    private JButton []memoryDisplay;//物理内存显示
    private infoPanel infoPanel;//信息面板，用于传参

    public memoryPanel(int capacity,int pageNum,int instructionNum,infoPanel infoPanel)
    {
        setLayout(new GridLayout(2,2));
        memoryBlocks=new page[4];
        memoryDisplay=new JButton[4];
        Font f=new Font("Helvetica",Font.BOLD,20);

        for(int i=0;i<4;i++)
        {
            memoryBlocks[i]=new page(-1,null,null);
            memoryDisplay[i]=new JButton("BLOCK"+i+" PAGE"+memoryBlocks[i].pageID);
            memoryDisplay[i].setEnabled(false);
            memoryDisplay[i].setFont(f);
            add(memoryDisplay[i]);
        }//画出页面

        this.capacity = capacity;
        this.pageNum = pageNum;
        this.instructionNum = instructionNum;
        this.infoPanel=infoPanel;
        size = 0;
        head = null;
        tail = null;
    }

    public void changeDisplay(int block)
    {
        memoryDisplay[block].setText("BLOCK"+block+" PAGE"+memoryBlocks[block].pageID);
        infoPanel.missCountDisplay.setText("Page Missing Count:"+Integer.toString(this.miss));//缺页数
        infoPanel.missRateDisplay.setText("Page Missing Rate:"+String.format("%.6f",(double)miss/320));//缺页率
    }//实时改变内存块显示信息及缺页数、缺页率

    public boolean isFull()
    {
        return size==4;
    }//判断内存是否已满

    public void addTail(int instructionID)
    {
        int pageIndex=instructionID/10;

        if (tail== null)
        {
            page page = new page(pageIndex,null,null);
            this.head=page;
            this.tail=page;
        }
        else
        {
            page page=new page(pageIndex,tail,null);
            this.tail.next=page;
            this.tail=page;
        }
        size++;

    }//插入链表尾部

    public void removeHead()
    {
        if(size==1)
        {
            head=null;
            tail=null;
        }
        else
        {
            page newHead=head.next;
            newHead.prev=null;
            head=newHead;
        }
        size--;
    }//移除链表头部

    public void removeTail()
    {
        if(size==1)
        {
            head=null;
            tail=null;
        }
        else
        {
            page newTail=tail.prev;
            newTail.next=null;
            tail=newTail;
        }
        size--;
    }//移除链表尾部

    public void remove(int instructionID)
    {
        int pageIndex=instructionID/10;

        if(find(instructionID)!=null)
        {
            page target=find(instructionID);
            if(target.prev!=null&&target.next!=null)
            {
                target.prev.next=target.next;
                target.next.prev=target.prev;
            }
            else if(target.prev==null&&target.next!=null)
            {
               removeHead();
               size++;
            }
            else if(target.prev!=null&&target.next==null)
            {
                removeTail();
                size++;
            }
            else
            {
                head=null;
                tail=null;
            }
        }
        size--;
    }//移除链表中某节点


    public page find(int instructionID)//在链表中寻找指令号instructionID所在页面
    {
        int pageIndex=instructionID/10;
        page index=this.head;
        while(index!=null)
        {
            if(index.pageID==pageIndex)
                break;
            index=index.next;
        }
        return index;
    }

    public int findBlock(int pageID)//在物理内存中寻找页面位置
    {
        int block=-1;
        for(int i=0;i<size;i++)
        {
            if(memoryBlocks[i].pageID==pageID)
            {
                block=i;
                break;
            }
        }
        return block;
    }

    public void execute(int instructionID)//执行某条指令
    {
        int pageID=instructionID/10;//根据指令号计算页面
        page currentInstruction=find(instructionID);
        if(currentInstruction!=null)//访问指令在内存中
        {
            remove(instructionID);
            addTail(instructionID);//因为最近访问该指令，改变页面在链表中的位置

            String text="Instruction "+instructionID+" from page "+pageID+" is already in memory block "+findBlock(pageID)+"\n";
            infoPanel.cacheInfo.append(text);
        }

        else//访问指令不在内存中
        {
            miss++;//缺页率加一

            if(isFull())//若内存满
            {
                int oldBlockID=findBlock(head.pageID);//要调出的页面所在的物理内存位置
                int oldPageID=memoryBlocks[oldBlockID].pageID;//旧页号

                removeHead();//最久未访问的页出队
                addTail(instructionID);//新页入队
                memoryBlocks[oldBlockID]=tail;//页面置换

                changeDisplay(oldBlockID);
                String text="Visit instruction "+instructionID+",memory is full,page "+oldPageID+" in block "+oldBlockID+" is replaced by page "+pageID+"\n";
                infoPanel.cacheInfo.append(text);

            }

            else//内存不满
            {
                addTail(instructionID);//插入链表尾部
                memoryBlocks[size-1]=tail;

                changeDisplay(size-1);
                String text="Visit instruction "+instructionID+" ,page "+pageID+" is put in block "+(size-1)+"\n";
                infoPanel.cacheInfo.append(text);
            }

        }

        try
        {
            Thread.sleep(100);
        }
        catch (Exception e)
        {
        }
    }

    public void lru()
    {
        int m=(int)(Math.random()*319);  ;//起始指令编号
        int count=0;
        execute(m);
        count++;
        execute(m+1);
        count++;

        while(count<instructionNum)//指令执行320次后停止
        {
            int m1=(int)(Math.random()*m);
            execute(m1);
            count++;
            execute(m1+1);
            count++;
            int m2=(int)(Math.random()*(320-m1-2))+m1+1;
            execute(m2);
            count++;
            execute(m2+1);
            count++;
        }

    }

}


