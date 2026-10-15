package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.EarthBorder
import com.example.ui.theme.EarthSurfaceVariant

@Composable
fun ActionPlaybookCard(
    actions: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EarthSurfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, EarthBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // Indian Cyber Crime Helpline Banner (1930)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2C2013), RoundedCornerShape(8.dp))
                .border(1.dp, AmberPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(AmberPrimary, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Helpline",
                    tint = Color(0xFF1E1000),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Reported a financial scam? Dial 1930",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = AmberPrimary
                    )
                )
                Text(
                    text = "National Cyber Crime Helpline • cybercrime.gov.in",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = Color(0xFFFAF5EF)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Recommended Action Steps:",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        actions.forEach { action ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Check",
                    tint = AmberPrimary,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = action,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFE5DDD3),
                        lineHeight = 17.sp
                    )
                )
            }
        }
    }
}
