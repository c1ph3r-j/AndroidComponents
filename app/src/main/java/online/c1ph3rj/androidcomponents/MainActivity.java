package online.c1ph3rj.androidcomponents;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

import online.c1ph3rj.androidcomponents.core.GridAdapter;
import online.c1ph3rj.androidcomponents.model.GridItem;
import online.c1ph3rj.easycamera.ui.CameraUI;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
    }

    void init() {
        try {
            RecyclerView recyclerView = findViewById(R.id.pagesView);

            // 2 columns grid
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

            List<GridItem> gridItems = List.of(
                    new GridItem("Camera", CameraUI.class)
            );

            GridAdapter adapter = new GridAdapter(this, gridItems);
            recyclerView.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}