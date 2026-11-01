package com.example.atlantvpn

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun ServerGlyph(icon:ImageVector,modifier:Modifier=Modifier,bright:Boolean=false) {
    Box(modifier.size(44.dp).background(if(bright)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center) {
        Icon(icon,null,Modifier.size(22.dp),tint=if(bright)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ServerStatusPill(text:String,emphasized:Boolean=false) {
    Surface(shape=RoundedCornerShape(50),color=if(emphasized)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(text,Modifier.padding(horizontal=10.dp,vertical=5.dp),style=MaterialTheme.typography.labelSmall,color=if(emphasized)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
    }
}

@Composable
internal fun ManagementDestination(title:String,detail:String,icon:ImageVector,onClick:()->Unit,modifier:Modifier=Modifier) {
    Surface(onClick=onClick,modifier=modifier,shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {ServerGlyph(icon);Icon(Icons.Default.ChevronRight,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)}
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {Text(title,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis);Text(detail,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)}
        }
    }
}

@Composable
internal fun ManagementMetric(label:String,value:String,modifier:Modifier=Modifier) {
    Column(modifier.padding(vertical=4.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {Text(value,style=MaterialTheme.typography.headlineSmall);Text(label,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
}

@Composable
internal fun CompactManagementSearch(value:String,onValueChange:(String)->Unit,modifier:Modifier=Modifier,enabled:Boolean=true) {
    val colors=MaterialTheme.colorScheme
    Surface(modifier=modifier,shape=RoundedCornerShape(12.dp),color=colors.surfaceContainerLow) {
        BasicTextField(value=value,onValueChange=onValueChange,enabled=enabled,singleLine=true,
            textStyle=MaterialTheme.typography.bodyMedium.copy(color=colors.onSurface),cursorBrush=SolidColor(colors.primary),
            keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),
            modifier=Modifier.fillMaxWidth().heightIn(min=40.dp).padding(horizontal=12.dp,vertical=9.dp).semantics{contentDescription=tr("Поиск")},
            decorationBox={input->Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Search,null,Modifier.size(17.dp),tint=colors.onSurfaceVariant)
                Box(Modifier.weight(1f)){if(value.isEmpty())Text(tr("Поиск"),style=MaterialTheme.typography.bodyMedium,color=colors.onSurfaceVariant);input()}
            }})
    }
}
