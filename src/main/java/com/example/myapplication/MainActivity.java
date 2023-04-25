package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout1);
    }
    private boolean isFileManagerInitialized ;

    private boolean[] selection;
    private File[] files;

    private List<String> filesList;
    private int filesFoundCount;

    private Button refreshButton;

    private File dir;

    private String currentPath;

    private boolean isLongClick;

    private int selectedItemIndex;

    private String copyPath;

    @Override
    protected void onResume(){
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionsDenided()){
            requestPermissions(PRRMISSIONS, REQUEST_PERMISSIONS );
            return;
        }

        if (!isFileManagerInitialized) {
            currentPath = String.valueOf(Environment.getExternalStoragePublicDirectory
                    (Environment.DIRECTORY_DOWNLOADS));
            final String   rootPath = currentPath.substring(0,currentPath.lastIndexOf('/'))  ;
            dir = new File(currentPath);
            files = dir.listFiles();
            final TextView pathOutput = findViewById(R.id.pathOutput);
            pathOutput.setText(currentPath.substring(currentPath.lastIndexOf('/') + 1));
            filesFoundCount = files.length;

            final ListView listView = findViewById(R.id.listView);
            final TextAdapter textAdapter1 = new TextAdapter();
            listView.setAdapter(textAdapter1);

            filesList = new ArrayList<>();

            for (int i = 0; i < filesFoundCount; i++) {
                filesList.add(String.valueOf(files[i].getAbsoluteFile()));
            }
            textAdapter1.setData(filesList);
            selection = new boolean[files.length];

            refreshButton = findViewById(R.id.refresh);
            refreshButton.setOnClickListener((v)->{
                files = dir.listFiles();
                if (files != null) {
                    filesFoundCount = files.length;
                } else {
                    filesFoundCount = 0;
                }
                filesList.clear();
                for (int i = 0; i < filesFoundCount; i++) {
                    filesList.add(String.valueOf(files[i].getAbsolutePath()));
                }

                textAdapter1.setData(filesList);

            });

            final Button goBackButton = findViewById(R.id.goBack);
            goBackButton.setOnClickListener((v)-> {

                if (currentPath.equals(rootPath)){
                    return;
                }
                currentPath = currentPath.substring(0,currentPath.lastIndexOf('/'))  ;
                dir = new File(currentPath);
                pathOutput.setText(currentPath.substring(currentPath.lastIndexOf('/') + 1));
                refreshButton.callOnClick();

                selection = new boolean[files.length];
                textAdapter1.setSlection(selection);
            });

            listView.setOnItemClickListener((parent, view, position, id)-> {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isLongClick){
                            if (position>files.length){
                                return;
                            }
                            if (files[position].isDirectory()){
                                currentPath = files[position].getAbsolutePath();
                                dir = new File(currentPath);
                                pathOutput.setText(currentPath.substring(currentPath.lastIndexOf('/') + 1));

                                refreshButton.callOnClick();

                                selection = new boolean[files.length];
                                textAdapter1.setSlection(selection);
                            }
                        }
                    }
                },50);
            });

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    isLongClick =  true;
                    selection[position] = !selection[position];
                    textAdapter1.setSlection(selection);
                    int selectionCount =0;

                    for (boolean aSelection : selection) {
                        if (aSelection) {
                           selectionCount++;
                        }
                    }
                    if (selectionCount>0) {
                        if (selectionCount == 1){
                            selectedItemIndex = position;
                            findViewById(R.id.rename).setVisibility(View.VISIBLE);
                    }else {
                            findViewById(R.id.rename).setVisibility(View.GONE);
                        }
                        findViewById(R.id.bottomBar).setVisibility(View.VISIBLE);
                    } else {
                        findViewById(R.id.bottomBar).setVisibility(View.GONE);
                    }
                    new Handler().postDelayed(new Runnable(){
                        @Override
                        public void run(){
                            isLongClick = false;
                        }
                    },1000);
                    return false;
                }
            });

            final Button b1 = findViewById(R.id.b1);
            final Button b2 = findViewById(R.id.rename);
            final Button b3 = findViewById(R.id.copy);
            final Button b4 = findViewById(R.id.paste);


            b1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(MainActivity.this);
                    deleteDialog.setTitle("Delete");
                    deleteDialog.setMessage("Do you really want to Delete it?");
                    deleteDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for (int i = 0; i < files.length; i++) {
                                if (selection[i]) {
                                    deleteFileOrFolder(files[i]);
                                    selection[i] = false;
                                }
                            }
                            refreshButton.callOnClick();
                        }
                    });
                    deleteDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    deleteDialog.show();
                }
            });
            final Button createNewFolder = findViewById(R.id.newFolder);
            createNewFolder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder newFolderDialog =new AlertDialog.Builder
                            (MainActivity.this);
                    newFolderDialog.setTitle("New Folder");
                    final EditText input = new EditText(MainActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    newFolderDialog.setView(input);
                    newFolderDialog.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final File newFolder = new File(currentPath+"/"+input.getText());
                                    if (!newFolder.exists()){
                                        newFolder.mkdir();
                                        refreshButton.callOnClick();
                                    }
                                }
                            });
                    newFolderDialog.setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    newFolderDialog.show();
                }
            });
            final Button renameButton = findViewById(R.id.rename);
            renameButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder renameDiallog=
                            new AlertDialog.Builder(MainActivity.this);
                    renameDiallog.setTitle("Rename to:");
                    final EditText input= new EditText(MainActivity.this);
                  final String renamePath = files[selectedItemIndex].getAbsolutePath();
                    input.setText(renamePath.substring(renamePath.lastIndexOf('/')));
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    renameDiallog.setView(input);
                    renameDiallog.setPositiveButton("Rename",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String s = new File(renamePath).getParent() + "/"+input.getText();
                                    File newFile = new File(s);
                                    new File(renamePath).renameTo(newFile);
                                    refreshButton.callOnClick();
                                    selection = new boolean[files.length];
                                    textAdapter1.setSlection(selection);

                                }
                            });
                    renameDiallog.show();
                }
            });

            final Button copyButton = findViewById(R.id.copy);
            copyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    copyPath = files[selectedItemIndex].getAbsolutePath();
                   selection = new boolean[files.length];
                    textAdapter1.setSlection(selection) ;
                    findViewById(R.id.paste).setVisibility(View.VISIBLE);
                }
            });

            final Button pasteButton = findViewById(R.id.paste);
            pasteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pasteButton.setVisibility(View.GONE);
                    String dstPath = currentPath + copyPath.substring(copyPath.lastIndexOf('/'));
                //  Log.d("test", dstPath);
                  copy(new File(copyPath), new File(dstPath));
                  files = new File(currentPath).listFiles();
                  selection = new boolean[files.length];
                  textAdapter1.setSlection(selection);
                   refreshButton.callOnClick();

                }
            });

            isFileManagerInitialized =true;
        }else {
            refreshButton.callOnClick();
        }
    }

    private void copy(File src, File dst){
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);
            byte[] buf = new byte[1024];
            int len;
            while ((len= in.read(buf))>0){
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class TextAdapter extends BaseAdapter{
        private List<String> data = new ArrayList<>();

        private boolean[] slection;


        public void setData(List<String> data){
            if (data!=null){
                this.data.clear();
                if (data.size()>0){
                    this.data.addAll(data);
                }
                notifyDataSetChanged();
            }
        }

        void setSlection(boolean[]slection){
            if (slection!=null){
                this.slection=new boolean[selection.length];
                for (int i=0;i<selection.length;i++){
                   this.slection[i]=selection[i];
                }
                notifyDataSetChanged();
            }
        }
        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public String getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView==null){
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
                convertView.setTag(new ViewHolder((TextView) convertView.findViewById(R.id.textItem) ));
            }
            ViewHolder holder =(ViewHolder) convertView.getTag();
            final String item = getItem(position);
            holder.info.setText(item.substring(item.lastIndexOf('/')+1));
            if (selection!=null){

                if (selection[position]){
                    holder.info.setBackgroundColor(Color.argb(20,9,4,9));
                }else{
                    holder.info.setBackgroundColor(Color.WHITE);
                }

            }
            return convertView;
        }
        class ViewHolder{
            TextView info;

            ViewHolder(TextView info){
                this.info = info;
            }
        }
    }

    private static final int REQUEST_PERMISSIONS = 1234;

    private static final String[] PRRMISSIONS ={
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private  static final int PERMISSIONS_COUNT =2;

    private boolean arePermissionsDenided(){

            int p = 0;
            while (p<PERMISSIONS_COUNT){
              if ( checkSelfPermission(PRRMISSIONS[p]) != PackageManager.PERMISSION_GRANTED){
                    return true;
                }
              p++;
            }

        return false;
    }



        private void deleteFileOrFolder(File fileOrFolder){
        if (fileOrFolder.isDirectory()){
            if (fileOrFolder.list().length==0){
                fileOrFolder.delete();
            }else {
                String files[] = fileOrFolder.list();
                for (String temp : files) {
                    File fileToDelete = new File(fileOrFolder, temp);
                    deleteFileOrFolder(fileToDelete);
                }
                if (fileOrFolder.list().length == 0) {
                    fileOrFolder.delete();
                }
            }
            }else{
                fileOrFolder.delete();
            }
        }


    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions,
                                           final int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
// gotta check why the user permission is not asked everytime!!
        if (requestCode==REQUEST_PERMISSIONS && grantResults.length>0)  {
            if (arePermissionsDenided()){
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            }
            else {
                onResume();
            }
        }

    }
}